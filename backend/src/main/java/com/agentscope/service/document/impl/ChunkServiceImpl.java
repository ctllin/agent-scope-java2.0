package com.agentscope.service.document.impl;

import com.agentscope.service.document.ChunkService;

import com.agentscope.common.enums.DocumentStatus;
import com.agentscope.common.enums.SplitStrategy;
import com.agentscope.config.RagConfig;
import com.agentscope.model.entity.DocumentChunk;
import com.agentscope.model.entity.KnowledgeDocument;
import com.agentscope.repository.ChunkRepository;
import com.agentscope.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 文档分块服务
 * 负责将文档内容分割为可独立检索的文本块
 * 支持段落分割和字符分割（带重叠）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkServiceImpl implements ChunkService {

    private final ChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final RagConfig ragConfig;

    /**
     * 根据文档内容自动选择分割策略进行分块
     */
    public List<DocumentChunk> splitContent(String documentId, String knowledgeBaseId, String content) {
        return splitContent(documentId, knowledgeBaseId, content, SplitStrategy.AUTO, null, null);
    }

    /**
     * 根据指定的分割策略进行分块
     */
    public List<DocumentChunk> splitContent(String documentId, String knowledgeBaseId, String content,
                                             SplitStrategy strategy, Integer chunkSize, Double overlapRatio) {
        return splitContent(documentId, knowledgeBaseId, content, strategy, chunkSize, overlapRatio, null);
    }

    /**
     * 根据指定的分割策略进行分块（支持手动分块分隔符）
     */
    public List<DocumentChunk> splitContent(String documentId, String knowledgeBaseId, String content,
                                             SplitStrategy strategy, Integer chunkSize, Double overlapRatio,
                                             String delimiter) {
        List<String> chunkTexts;
        String strategyName;

        // 自动选择策略
        SplitStrategy actualStrategy = strategy;
        if (actualStrategy == null || actualStrategy == SplitStrategy.AUTO) {
            if (content.contains("\n\n") || Pattern.compile("#+\\s", Pattern.MULTILINE).matcher(content).find()) {
                actualStrategy = SplitStrategy.PARAGRAPH;
            } else {
                actualStrategy = SplitStrategy.CHARACTERS;
            }
        }

        if (actualStrategy == SplitStrategy.PARAGRAPH) {
            chunkTexts = splitByParagraph(content, chunkSize, overlapRatio);
            strategyName = SplitStrategy.PARAGRAPH.getCode();
        } else if (actualStrategy == SplitStrategy.MANUAL) {
            chunkTexts = splitByDelimiter(content, delimiter);
            strategyName = SplitStrategy.MANUAL.getCode();
        } else {
            chunkTexts = splitByCharacters(content, chunkSize, overlapRatio);
            strategyName = SplitStrategy.CHARACTERS.getCode();
        }

        // 获取文档名称
        String documentName = documentRepository.findById(documentId)
                .map(KnowledgeDocument::getName)
                .orElse(null);

        List<DocumentChunk> result = new ArrayList<>();
        for (int i = 0; i < chunkTexts.size(); i++) {
            DocumentChunk chunk = DocumentChunk.builder()
                    .documentId(documentId)
                    .documentName(documentName)
                    .knowledgeBaseId(knowledgeBaseId)
                    .content(chunkTexts.get(i).trim())
                    .sequence(i)
                    .splitStrategy(strategyName)
                    .embedded(false)
                    .build();
            result.add(chunk);
        }

        return result;
    }

    /**
     * 按段落分割，保留段落结构完整性
     */
    private List<String> splitByParagraph(String content, Integer chunkSize, Double overlapRatio) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = content.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        int maxSize = chunkSize != null ? chunkSize : ragConfig.getChunkSize();
        double ratio = overlapRatio != null ? overlapRatio : ragConfig.getOverlapRatio();

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() > maxSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                int overlapStart = Math.max(0, currentChunk.length() - (int) (maxSize * ratio));
                currentChunk = new StringBuilder(currentChunk.substring(overlapStart));
                currentChunk.append("\n\n");
            }
            currentChunk.append(trimmed).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 按固定字符数分割，相邻分块保留指定比例的重叠
     */
    private List<String> splitByCharacters(String content, Integer chunkSize, Double overlapRatio) {
        List<String> chunks = new ArrayList<>();
        int size = chunkSize != null ? chunkSize : ragConfig.getChunkSize();
        double ratio = overlapRatio != null ? overlapRatio : ragConfig.getOverlapRatio();
        int overlap = (int) (size * ratio);

        for (int i = 0; i < content.length(); i += size - overlap) {
            int end = Math.min(i + size, content.length());
            String chunk = content.substring(i, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    /**
     * 按自定义分隔符分割（手动分块）
     */
    private List<String> splitByDelimiter(String content, String delimiter) {
        List<String> chunks = new ArrayList<>();
        if (delimiter == null || delimiter.isBlank()) {
            delimiter = "\n";
        }
        String[] parts = content.split(delimiter, -1);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                chunks.add(trimmed);
            }
        }
        return chunks;
    }

    /**
     * 获取指定文档下所有未删除的分块
     */
    public List<DocumentChunk> getByDocumentId(String documentId) {
        return chunkRepository.findByDocumentIdAndDelFlag(documentId, "NOT_DELETED");
    }

    /**
     * 查询指定知识库下所有未删除的分块
     */
    public List<DocumentChunk> getByKnowledgeBaseId(String knowledgeBaseId) {
        return chunkRepository.findByKnowledgeBaseIdAndDelFlag(knowledgeBaseId, "NOT_DELETED");
    }

    /**
     * 获取指定分块
     */
    public DocumentChunk getById(String chunkId) {
        return chunkRepository.findById(chunkId).orElse(null);
    }

    /**
     * 删除指定分块（逻辑删除）
     */
    @Transactional
    public void deleteById(String chunkId) {
        DocumentChunk chunk = chunkRepository.findById(chunkId).orElse(null);
        if (chunk == null || "DELETED".equals(chunk.getDelFlag())) return;

        chunk.setDelFlag("DELETED");
        chunkRepository.save(chunk);

        // 更新文档分块数
        refreshChunkCount(chunk.getDocumentId());
    }

    /**
     * 批量删除分块（逻辑删除）
     */
    @Transactional
    public void batchDelete(List<String> chunkIds) {
        List<DocumentChunk> chunks = chunkRepository.findByIdInAndDelFlag(chunkIds, "NOT_DELETED");
        if (chunks.isEmpty()) return;

        for (DocumentChunk chunk : chunks) {
            chunk.setDelFlag("DELETED");
        }
        chunkRepository.saveAll(chunks);

        // 按文档分组，批量更新 chunkCount
        Map<String, List<DocumentChunk>> byDoc = chunks.stream()
                .collect(Collectors.groupingBy(DocumentChunk::getDocumentId));
        byDoc.keySet().forEach(this::refreshChunkCount);
    }

    /**
     * 清除指定文档的所有分块（逻辑删除）
     */
    @Transactional
    public void clearByDocumentId(String documentId) {
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdAndDelFlag(documentId, "NOT_DELETED");
        for (DocumentChunk chunk : chunks) {
            chunk.setDelFlag("DELETED");
        }
        chunkRepository.saveAll(chunks);
        refreshChunkCount(documentId);
    }

    /**
     * 合并多个分块为单个分块
     */
    @Transactional
    public DocumentChunk mergeChunks(List<String> chunkIds, String documentId, String knowledgeBaseId) {
        List<DocumentChunk> chunks = chunkRepository.findByIdInAndDelFlag(chunkIds, "NOT_DELETED");
        StringBuilder merged = new StringBuilder();
        int minSeq = chunks.stream().mapToInt(DocumentChunk::getSequence).min().orElse(0);

        for (DocumentChunk chunk : chunks) {
            merged.append(chunk.getContent()).append("\n\n");
            chunk.setDelFlag("DELETED");
        }
        chunkRepository.saveAll(chunks);

        String documentName = documentRepository.findById(documentId)
                .map(KnowledgeDocument::getName)
                .orElse(null);

        DocumentChunk mergedChunk = DocumentChunk.builder()
                .documentId(documentId)
                .documentName(documentName)
                .knowledgeBaseId(knowledgeBaseId)
                .content(merged.toString().trim())
                .sequence(minSeq)
                .splitStrategy(SplitStrategy.MERGED.getCode())
                .embedded(false)
                .build();

        DocumentChunk saved = chunkRepository.save(mergedChunk);
        refreshChunkCount(documentId);
        return saved;
    }

    /**
     * 重新计算文档的未删除分块数量，并更新文档的 chunkCount
     */
    private void refreshChunkCount(String documentId) {
        if (documentId == null) return;
        long count = chunkRepository.countByDocumentIdAndDelFlag(documentId, "NOT_DELETED");
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setChunkCount((int) count);
            if (count == 0) {
                doc.setStatus(DocumentStatus.UPLOADED);
            } else {
                doc.setStatus(DocumentStatus.SPLIT);
            }
            documentRepository.save(doc);
        });
    }

    // ========== 手动分块 ==========

    /**
     * 创建单个手动分块
     */
    @Transactional
    public DocumentChunk createManualChunk(String documentId, String content, int sequence) {
        KnowledgeDocument doc = documentRepository.findById(documentId).orElse(null);
        DocumentChunk chunk = DocumentChunk.builder()
                .documentId(documentId)
                .documentName(doc != null ? doc.getName() : null)
                .knowledgeBaseId(doc != null ? doc.getKnowledgeBaseId() : null)
                .content(content)
                .sequence(sequence)
                .splitStrategy(SplitStrategy.MANUAL.getCode())
                .embedded(false)
                .build();
        DocumentChunk saved = chunkRepository.save(chunk);
        refreshChunkCount(documentId);
        return saved;
    }

    /**
     * 更新分块内容
     */
    @Transactional
    public DocumentChunk updateChunkContent(String chunkId, String content) {
        DocumentChunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new RuntimeException("分块不存在: " + chunkId));
        chunk.setContent(content);
        return chunkRepository.save(chunk);
    }

    /**
     * 批量更新分块序号（拖拽排序后调用）
     */
    @Transactional
    public void reorderChunks(List<Map<String, Object>> orderList) {
        for (Map<String, Object> item : orderList) {
            String id = (String) item.get("id");
            Object seqObj = item.get("sequence");
            int seq = seqObj instanceof Number ? ((Number) seqObj).intValue() : 0;
            chunkRepository.findById(id).ifPresent(c -> {
                c.setSequence(seq);
                chunkRepository.save(c);
            });
        }
    }

    /**
     * 批量保存手动分块（清空旧分块后创建新分块）
     */
    @Transactional
    public List<DocumentChunk> saveManualChunks(String documentId, String knowledgeBaseId,
                                                 List<Map<String, Object>> chunkDataList) {
        // 1. 清空旧分块
        clearByDocumentId(documentId);

        // 2. 创建新分块
        List<DocumentChunk> result = new ArrayList<>();
        for (int i = 0; i < chunkDataList.size(); i++) {
            Map<String, Object> data = chunkDataList.get(i);
            String content = (String) data.get("content");
            if (content == null || content.isBlank()) continue;

            DocumentChunk chunk = DocumentChunk.builder()
                    .documentId(documentId)
                    .knowledgeBaseId(knowledgeBaseId)
                    .content(content)
                    .sequence(i)
                    .splitStrategy(SplitStrategy.MANUAL.getCode())
                    .embedded(false)
                    .build();
            result.add(chunk);
        }
        result = chunkRepository.saveAll(result);
        refreshChunkCount(documentId);
        return result;
    }
}
