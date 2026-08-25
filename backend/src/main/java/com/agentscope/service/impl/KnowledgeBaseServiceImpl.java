package com.agentscope.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.agentscope.common.ResultCode;
import com.agentscope.common.enums.DocumentStatus;
import com.agentscope.common.enums.DocumentType;
import com.agentscope.common.enums.OcrStatus;
import com.agentscope.common.enums.SplitStrategy;
import com.agentscope.common.exception.BusinessException;
import com.agentscope.config.MilvusConfig;
import com.agentscope.model.dto.CreateKnowledgeBaseRequest;
import com.agentscope.model.entity.DocumentChunk;
import com.agentscope.model.entity.KnowledgeDocument;
import com.agentscope.model.entity.KnowledgeBase;
import com.agentscope.model.entity.ChatSession;
import com.agentscope.model.entity.ChatMessage;
import com.agentscope.model.vo.DocumentVO;
import com.agentscope.model.vo.KnowledgeBaseVO;
import com.agentscope.model.vo.SearchResultVO;
import com.agentscope.repository.ChunkRepository;
import com.agentscope.repository.DocumentRepository;
import com.agentscope.repository.KnowledgeBaseRepository;
import com.agentscope.repository.ChatSessionRepository;
import com.agentscope.repository.ChatMessageRepository;
import com.agentscope.service.EmbeddingService;
import com.agentscope.service.XFileStorageService;
import com.agentscope.service.KnowledgeBaseService;
import com.agentscope.service.MilvusService;
import com.agentscope.service.document.ChunkService;
import com.agentscope.service.document.FileParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MilvusService milvusService;
    private final EmbeddingService embeddingService;
    private final MilvusConfig milvusConfig;
    private final XFileStorageService xFileStorage;
    private final FileParseService fileParseService;
    private final ChunkService chunkService;
    private final MongoTemplate mongoTemplate;

    @Override
    public KnowledgeBaseVO createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        if (knowledgeBaseRepository.existsByName(request.getName())) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "知识库名称已存在");
        }

        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .name(request.getName())
                .description(request.getDescription())
                .icon(request.getIcon())
                .documentCount(0)
                .vectorCount(0)
                .build();
        knowledgeBase = knowledgeBaseRepository.save(knowledgeBase);

        String collectionName = getCollectionName(knowledgeBase.getId());
        try {
            milvusService.createCollection(collectionName, milvusConfig.getDimension());
        } catch (Exception e) {
            log.error("创建Milvus Collection失败: {}", collectionName, e);
        }

        return convertToVO(knowledgeBase);
    }

    @Override
    public void deleteKnowledgeBase(String id) {
        if (!knowledgeBaseRepository.existsById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "知识库不存在");
        }

        // 1. 删除 Milvus 向量集合
        String collectionName = getCollectionName(id);
        try {
            milvusService.dropCollection(collectionName);
        } catch (Exception e) {
            log.error("删除Milvus Collection失败: {}", collectionName, e);
        }

        // 2. 删除关联的对话消息和会话
        List<ChatSession> sessions = chatSessionRepository.findByKnowledgeBaseId(id);
        for (ChatSession session : sessions) {
            List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAt(session.getId());
            chatMessageRepository.deleteAll(messages);
        }
        chatSessionRepository.deleteAll(sessions);

        // 3. 删除分块
        List<DocumentChunk> chunks = chunkRepository.findByKnowledgeBaseId(id);
        chunkRepository.deleteAll(chunks);

        // 4. 删除文档
        List<KnowledgeDocument> documents = documentRepository.findByKnowledgeBaseId(id);
        documentRepository.deleteAll(documents);

        // 5. 删除文件
        xFileStorage.deleteKnowledgeBaseFiles(id);

        // 6. 删除知识库本身
        knowledgeBaseRepository.deleteById(id);
    }

    @Override
    public List<KnowledgeBaseVO> listAllKnowledgeBases() {
        return knowledgeBaseRepository.findAll().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public KnowledgeBaseVO getKnowledgeBaseById(String id) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "知识库不存在"));
        return convertToVO(knowledgeBase);
    }

    /**
     * 上传文档到知识库（不自动分块，只存储文件+解析内容）
     */
    @Override
    public String uploadDocument(String knowledgeBaseId, MultipartFile file) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "知识库不存在"));

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        DocumentType documentType = DocumentType.fromExtension(extension);
        if (documentType == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的文件类型");
        }

        // 同名文档覆盖：先完整清理旧文档（向量+分块+磁盘文件+计数）
        List<KnowledgeDocument> duplicates = documentRepository.findByKnowledgeBaseIdAndName(knowledgeBaseId, originalFilename);
        for (KnowledgeDocument dup : duplicates) {
            try {
                deleteDocument(dup.getId());
                log.info("上传覆盖同名文档: knowledgeBaseId={}, name={}, removedId={}",
                        knowledgeBaseId, originalFilename, dup.getId());
            } catch (Exception e) {
                log.error("清理同名旧文档失败: documentId={}", dup.getId(), e);
            }
        }

        String filePath;
        try {
            filePath = xFileStorage.storeFile(file, knowledgeBaseId);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件存储失败");
        }

        String content;
        List<KnowledgeDocument.PageContent> pageContents=new ArrayList<>();
        List<String> contents;
        try {
            File physicalFile = new File(xFileStorage.getFilePath(filePath).toString());
            content = fileParseService.parseFile(physicalFile, extension);
            contents = fileParseService.parseFileByPage(physicalFile, extension);
            if(!CollectionUtil.isEmpty(contents)){
                AtomicInteger i= new AtomicInteger();
                contents.forEach(pageText->{
                    KnowledgeDocument.PageContent pageContent=new KnowledgeDocument.PageContent();
                    pageContent.setText(pageText);
                    pageContent.setPage(i.incrementAndGet());
                    pageContents.add(pageContent);
                });
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件解析失败: " + e.getMessage());
        }

        KnowledgeDocument document = KnowledgeDocument.builder()
                .name(originalFilename)
                .type(documentType)
                .filePath(filePath)
                .content(content)
                .pageContents(pageContents)
                .fileSize(file.getSize())
                .knowledgeBaseId(knowledgeBaseId)
                .chunkCount(0)
                .embeddedCount(0)
                .status(DocumentStatus.UPLOADED)
                .ocrStatus(OcrStatus.NONE)
                .build();

        document = documentRepository.save(document);

        knowledgeBase.setDocumentCount(knowledgeBase.getDocumentCount() + 1);
        knowledgeBaseRepository.save(knowledgeBase);

        return document.getId();
    }

    /**
     * 对文档进行分块处理（清理旧分块+向量，再创建新分块）
     */
    @Override
    public List<DocumentChunk> splitDocument(String documentId, SplitStrategy strategy,
                                              Integer chunkSize, Double overlapRatio) {
        return splitDocument(documentId, strategy, chunkSize, overlapRatio, null);
    }

    /**
     * 对文档进行分块处理（支持手动分块分隔符）
     */
    public List<DocumentChunk> splitDocument(String documentId, SplitStrategy strategy,
                                              Integer chunkSize, Double overlapRatio, String delimiter) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "文档不存在"));

        // 1. 清理旧的向量数据（Milvus）
        String collectionName = getCollectionName(document.getKnowledgeBaseId());
        try {
            milvusService.deleteByDocumentId(collectionName, documentId);
        } catch (Exception e) {
            log.warn("清理旧向量失败: {}", e.getMessage());
        }

        // 2. 清理旧的分块（MongoDB逻辑删除）
        chunkService.clearByDocumentId(documentId);

        // 3. 执行新分块
        List<DocumentChunk> chunks = chunkService.splitContent(
                documentId, document.getKnowledgeBaseId(),
                document.getContent(), strategy, chunkSize, overlapRatio, delimiter);

        // 4. 保存新分块
        chunks = chunkRepository.saveAll(chunks);

        // 5. 更新文档状态
        document.setChunkCount(chunks.size());
        document.setEmbeddedCount(0);
        document.setStatus(DocumentStatus.SPLIT);
        documentRepository.save(document);

        // 6. 更新知识库向量数
        refreshKnowledgeBaseVectorCount(document.getKnowledgeBaseId());

        log.info("文档重新分块完成: documentId={}, chunks={}", documentId, chunks.size());
        return chunks;
    }

    /**
     * 获取文档的所有分块
     */
    @Override
    public List<DocumentChunk> getDocumentChunks(String documentId) {
        return chunkRepository.findByDocumentIdAndDelFlag(documentId, "NOT_DELETED");
    }

    @Override
    public List<DocumentVO> listDocuments(String knowledgeBaseId) {
        return documentRepository.findByKnowledgeBaseId(knowledgeBaseId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> listDocumentsPage(String knowledgeBaseId, String name, String status, String ocrStatus, int page, int size) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        
        Query query = new Query();
        query.addCriteria(Criteria.where("knowledgeBaseId").is(knowledgeBaseId));
        if (name != null && !name.isBlank()) {
            query.addCriteria(Criteria.where("name").regex(name, "i"));
        }
        if (status != null && !status.isBlank()) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (ocrStatus != null && !ocrStatus.isBlank()) {
            OcrStatus ocr = OcrStatus.fromCode(ocrStatus);
            if (ocr != null) {
                query.addCriteria(Criteria.where("ocrStatus").is(ocr));
            }
        }
        
        long total = mongoTemplate.count(query, KnowledgeDocument.class);
        query.with(pageable);
        List<KnowledgeDocument> docs = mongoTemplate.find(query, KnowledgeDocument.class);
        List<DocumentVO> records = docs.stream().map(this::convertToVO).collect(Collectors.toList());
        
        return Map.of(
                "total", total,
                "page", page,
                "size", size,
                "records", records
        );
    }

    /**
     * 删除文档（清理分块+向量+磁盘文件）
     */
    @Override
    public void deleteDocument(String documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "文档不存在"));

        // 清理Milvus向量
        String collectionName = getCollectionName(document.getKnowledgeBaseId());
        try {
            milvusService.deleteByDocumentId(collectionName, documentId);
        } catch (Exception e) {
            log.error("删除文档向量失败: documentId={}", documentId, e);
        }

        // 删除关联的分块
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndDelFlag(documentId, "NOT_DELETED");
        chunkRepository.deleteAll(chunks);

        // 删除磁盘文件
        if (document.getFilePath() != null) {
            xFileStorage.deleteFile(document.getFilePath());
        }

        // 更新知识库文档数量
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(document.getKnowledgeBaseId())
                .orElse(null);
        if (knowledgeBase != null) {
            knowledgeBase.setDocumentCount(Math.max(0, knowledgeBase.getDocumentCount() - 1));
            knowledgeBaseRepository.save(knowledgeBase);
        }

        documentRepository.deleteById(documentId);

        // 更新知识库向量数
        refreshKnowledgeBaseVectorCount(document.getKnowledgeBaseId());
    }

    @Override
    public void deleteDocuments(List<String> documentIds) {
        for (String documentId : documentIds) {
            try {
                deleteDocument(documentId);
            } catch (Exception e) {
                log.error("批量删除文档失败: documentId={}", documentId, e);
            }
        }
    }

    @Override
    public void batchSplitDocuments(List<String> documentIds) {
        for (String documentId : documentIds) {
            try {
                KnowledgeDocument doc = documentRepository.findById(documentId).orElse(null);
                if (doc == null || doc.getStatus() == DocumentStatus.SPLIT) {
                    continue;
                }
                splitDocument(documentId, SplitStrategy.PARAGRAPH, 500, 0.15, null);
            } catch (Exception e) {
                log.error("批量分块文档失败: documentId={}", documentId, e);
            }
        }
    }

    @Override
    public Path getDocumentFile(String documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "文档不存在"));
        if (document.getFilePath() == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "文件不存在");
        }
        return xFileStorage.getFilePath(document.getFilePath());
    }

    @Override
    public String getDocumentFileName(String documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "文档不存在"));
        return document.getName();
    }

    @Override
    public KnowledgeDocument getDocumentById(String documentId) {
        return documentRepository.findById(documentId).orElse(null);
    }

    @Override
    public void updateDocument(KnowledgeDocument document) {
        documentRepository.save(document);
    }

    @Override
    public List<SearchResultVO> searchVectors(String knowledgeBaseId, String query, int topK) {
        String collectionName = getCollectionName(knowledgeBaseId);
        if (!milvusService.hasCollection(collectionName)) {
            return List.of();
        }

        float[] queryVector = embeddingService.embed(query);
        List<Map<String, Object>> results = milvusService.search(collectionName, queryVector, topK, null);

        List<KnowledgeDocument> documents = documentRepository.findByKnowledgeBaseId(knowledgeBaseId);
        Map<String, String> docNameMap = documents.stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, KnowledgeDocument::getName));

        return results.stream()
                .map(result -> SearchResultVO.builder()
                        .content((String) result.get("content"))
                        .score(result.get("score") != null ? ((Number) result.get("score")).floatValue() : 0f)
                        .documentId((String) result.get("document_id"))
                        .documentName(docNameMap.getOrDefault((String) result.get("document_id"), "未知文档"))
                        .knowledgeBaseId(knowledgeBaseId)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 批量embedding分块
     */
    @Override
    public void batchEmbedChunks(List<String> chunkIds) {
        List<DocumentChunk> chunks = chunkRepository.findByIdInAndDelFlag(chunkIds, "NOT_DELETED");
        if (chunks.isEmpty()) return;

        // 按知识库分组处理
        Map<String, List<DocumentChunk>> byKb = chunks.stream()
                .collect(Collectors.groupingBy(DocumentChunk::getKnowledgeBaseId));

        for (Map.Entry<String, List<DocumentChunk>> entry : byKb.entrySet()) {
            String kbId = entry.getKey();
            List<DocumentChunk> kbChunks = entry.getValue();
            String collectionName = getCollectionName(kbId);

            if (!milvusService.hasCollection(collectionName)) {
                milvusService.createCollection(collectionName, milvusConfig.getDimension());
            }

            // 幂等防御：先清除本批分块在向量库中可能残留的旧向量。
            // 场景：分块内容被修改后重新嵌入、或批量向量化被重复触发，
            // 若不清理会产生新旧两份向量，检索将命中过期内容。
            List<String> kbChunkIds = kbChunks.stream()
                    .map(DocumentChunk::getId)
                    .collect(Collectors.toList());
            milvusService.deleteByChunkIds(collectionName, kbChunkIds);

            // 筛选未向量化的
            List<DocumentChunk> unembedded = kbChunks.stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getEmbedded()))
                    .collect(Collectors.toList());

            if (unembedded.isEmpty()) continue;

            try {
                // 逐条embedding并插入向量（保留chunk_id元数据）
                for (DocumentChunk chunk : unembedded) {
                    float[] embedding = embeddingService.embed(chunk.getContent());
                    milvusService.insertVectors(collectionName, kbId,
                            chunk.getDocumentId(), chunk.getId(),
                            List.of(chunk.getContent()), List.of(embedding));
                }

                // 标记已向量化
                for (DocumentChunk chunk : unembedded) {
                    chunk.setEmbedded(true);
                }
                chunkRepository.saveAll(unembedded);

                // 更新相关文档的embeddedCount（按文档分组）
                Map<String, List<DocumentChunk>> byDoc = unembedded.stream()
                        .collect(Collectors.groupingBy(DocumentChunk::getDocumentId));
                byDoc.keySet().forEach(this::refreshDocumentEmbeddedCount);

            } catch (Exception e) {
                log.error("批量embedding失败: kbId={}", kbId, e);
            }
        }

        // 更新知识库向量数
        for (String kbId : byKb.keySet()) {
            refreshKnowledgeBaseVectorCount(kbId);
        }
    }

    /**
     * 重新向量化文档：先清理该文档在Milvus中的全部旧向量，
     * 再重置分块标记并全量重建，避免内容变更后新旧向量并存
     */
    @Override
    public void reembedDocument(String documentId) {
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndDelFlag(documentId, "NOT_DELETED");
        if (chunks.isEmpty()) return;

        String kbId = chunks.get(0).getKnowledgeBaseId();
        String collectionName = getCollectionName(kbId);

        // 清理该文档的全部旧向量
        try {
            if (milvusService.hasCollection(collectionName)) {
                milvusService.deleteByDocumentId(collectionName, documentId);
            }
        } catch (Exception e) {
            log.error("清理文档旧向量失败: documentId={}", documentId, e);
        }

        // 重置向量化标记，使全部分块重新embedding
        chunks.forEach(c -> c.setEmbedded(false));
        chunkRepository.saveAll(chunks);
        refreshDocumentEmbeddedCount(documentId);

        batchEmbedChunks(chunks.stream().map(DocumentChunk::getId).collect(Collectors.toList()));
    }

    /**
     * 删除分块的向量数据
     */
    @Override
    public void deleteChunkVectors(List<String> chunkIds) {
        List<DocumentChunk> chunks = chunkRepository.findByIdInAndDelFlag(chunkIds, "NOT_DELETED");
        if (chunks.isEmpty()) return;

        Map<String, List<DocumentChunk>> byKb = chunks.stream()
                .collect(Collectors.groupingBy(DocumentChunk::getKnowledgeBaseId));

        for (Map.Entry<String, List<DocumentChunk>> entry : byKb.entrySet()) {
            String kbId = entry.getKey();
            String collectionName = getCollectionName(kbId);
            List<String> chunkIdList = entry.getValue().stream()
                    .map(DocumentChunk::getId).collect(Collectors.toList());

            try {
                milvusService.deleteByChunkIds(collectionName, chunkIdList);
            } catch (Exception e) {
                log.error("删除向量失败: kbId={}", kbId, e);
            }
        }

        // 重置embedded状态
        for (DocumentChunk chunk : chunks) {
            chunk.setEmbedded(false);
        }
        chunkRepository.saveAll(chunks);

        // 更新文档和知识库
        chunks.stream().map(DocumentChunk::getDocumentId).distinct()
                .forEach(this::refreshDocumentEmbeddedCount);
        byKb.keySet().forEach(this::refreshKnowledgeBaseVectorCount);
    }

    /**
     * 批量删除分块（含向量清理）
     */
    @Override
    public void batchDeleteChunks(List<String> chunkIds) {
        List<DocumentChunk> chunks = chunkRepository.findByIdInAndDelFlag(chunkIds, "NOT_DELETED");
        if (chunks.isEmpty()) return;

        // 按知识库分组清理向量
        Map<String, List<DocumentChunk>> byKb = chunks.stream()
                .collect(Collectors.groupingBy(DocumentChunk::getKnowledgeBaseId));

        for (Map.Entry<String, List<DocumentChunk>> entry : byKb.entrySet()) {
            String kbId = entry.getKey();
            String collectionName = getCollectionName(kbId);
            List<String> chunkIdList = entry.getValue().stream()
                    .map(DocumentChunk::getId).collect(Collectors.toList());

            try {
                milvusService.deleteByChunkIds(collectionName, chunkIdList);
            } catch (Exception e) {
                log.error("删除向量失败: kbId={}", kbId, e);
            }
        }

        // 逻辑删除分块
        for (DocumentChunk chunk : chunks) {
            chunk.setDelFlag("DELETED");
        }
        chunkRepository.saveAll(chunks);

        // 更新文档和知识库
        chunks.stream().map(DocumentChunk::getDocumentId).distinct()
                .forEach(docId -> {
                    refreshDocumentEmbeddedCount(docId);
                    refreshDocumentChunkCount(docId);
                });
        byKb.keySet().forEach(this::refreshKnowledgeBaseVectorCount);
    }

    /**
     * 合并分块（含向量清理）
     */
    @Override
    public DocumentChunk mergeChunks(List<String> chunkIds, String documentId, String knowledgeBaseId) {
        // 清理旧向量
        String collectionName = getCollectionName(knowledgeBaseId);
        try {
            milvusService.deleteByDocumentId(collectionName, documentId);
        } catch (Exception e) {
            log.warn("清理向量失败: {}", e.getMessage());
        }

        DocumentChunk merged = chunkService.mergeChunks(chunkIds, documentId, knowledgeBaseId);

        refreshDocumentEmbeddedCount(documentId);
        refreshDocumentChunkCount(documentId);
        refreshKnowledgeBaseVectorCount(knowledgeBaseId);

        return merged;
    }

    private void refreshDocumentEmbeddedCount(String documentId) {
        long count = chunkRepository.findByDocumentIdAndDelFlag(documentId, "NOT_DELETED").stream()
                .filter(c -> Boolean.TRUE.equals(c.getEmbedded()))
                .count();
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setEmbeddedCount((int) count);
            documentRepository.save(doc);
        });
    }

    private void refreshDocumentChunkCount(String documentId) {
        long count = chunkRepository.countByDocumentIdAndDelFlag(documentId, "NOT_DELETED");
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setChunkCount((int) count);
            if (count == 0) doc.setStatus(DocumentStatus.UPLOADED);
            documentRepository.save(doc);
        });
    }

    private void refreshKnowledgeBaseVectorCount(String knowledgeBaseId) {
        long count = chunkRepository.findByKnowledgeBaseIdAndDelFlag(knowledgeBaseId, "NOT_DELETED").stream()
                .filter(c -> Boolean.TRUE.equals(c.getEmbedded()))
                .count();
        knowledgeBaseRepository.findById(knowledgeBaseId).ifPresent(kb -> {
            kb.setVectorCount((int) count);
            knowledgeBaseRepository.save(kb);
        });
    }

    private String getCollectionName(String knowledgeBaseId) {
        return milvusConfig.getCollectionPrefix() + knowledgeBaseId;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private KnowledgeBaseVO convertToVO(KnowledgeBase knowledgeBase) {
        // 从文件系统统计实际文件数（新存储布局 doc/{kbId}/ 与旧布局均统计）
        int actualFileCount = 0;
        try {
            java.nio.file.Path kbDir = java.nio.file.Path.of("/data/agent-scope/storage/doc", knowledgeBase.getId());
            if (java.nio.file.Files.exists(kbDir) && java.nio.file.Files.isDirectory(kbDir)) {
                actualFileCount = (int) java.nio.file.Files.list(kbDir)
                        .filter(p -> java.nio.file.Files.isRegularFile(p))
                        .count();
            }
            if (actualFileCount == 0) {
                // 旧布局兜底
                kbDir = xFileStorage.getFilePath(knowledgeBase.getId());
                if (java.nio.file.Files.exists(kbDir) && java.nio.file.Files.isDirectory(kbDir)) {
                    try (var list = java.nio.file.Files.list(kbDir)) {
                        actualFileCount = (int) list.filter(java.nio.file.Files::isRegularFile).count();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("统计知识库文件数失败: {}", e.getMessage());
            actualFileCount = knowledgeBase.getDocumentCount();
        }

        return KnowledgeBaseVO.builder()
                .id(knowledgeBase.getId())
                .name(knowledgeBase.getName())
                .description(knowledgeBase.getDescription())
                .icon(knowledgeBase.getIcon())
                .documentCount(actualFileCount)
                .vectorCount(knowledgeBase.getVectorCount())
                .creatorId(knowledgeBase.getCreatorId())
                .createdAt(knowledgeBase.getCreatedAt())
                .build();
    }

    private DocumentVO convertToVO(KnowledgeDocument document) {
        return DocumentVO.builder()
                .id(document.getId())
                .name(document.getName())
                .type(document.getType())
                .fileSize(document.getFileSize())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .chunkCount(document.getChunkCount())
                .embeddedCount(document.getEmbeddedCount())
                .status(document.getStatus())
                .ocrStatus(document.getOcrStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
