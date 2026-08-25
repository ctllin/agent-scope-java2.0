package com.agentscope.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.agentscope.common.Response;
import com.agentscope.common.enums.OcrStatus;
import com.agentscope.config.FileStorageConfig;
import com.agentscope.config.TtsConfig;
import com.agentscope.events.BizEventType;
import com.agentscope.events.EventPublisher;
import com.agentscope.model.dto.CreateKnowledgeBaseRequest;
import com.agentscope.model.dto.SplitDocumentRequest;
import com.agentscope.model.entity.DocumentChunk;
import com.agentscope.model.entity.KnowledgeDocument;
import com.agentscope.model.vo.DocumentVO;
import com.agentscope.model.vo.KnowledgeBaseVO;
import com.agentscope.model.vo.SearchResultVO;
import com.agentscope.service.KnowledgeBaseService;
import com.agentscope.service.OcrService;
import com.agentscope.service.XFileStorageService;
import com.agentscope.util.PdfImageUtil;
import com.agentscope.service.document.ChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 知识库控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChunkService chunkService;
    private final OcrService ocrService;
    private final EventPublisher eventPublisher;
    private final FileStorageConfig fileStorageConfig;
    private final XFileStorageService xFileStorage;

    /**
     * 创建知识库
     */
    @PostMapping
    public Response<KnowledgeBaseVO> createKnowledgeBase(@RequestBody CreateKnowledgeBaseRequest request) {
        KnowledgeBaseVO vo = knowledgeBaseService.createKnowledgeBase(request);
        return Response.success(vo);
    }

    /**
     * 获取所有知识库
     */
    @GetMapping
    public Response<List<KnowledgeBaseVO>> listKnowledgeBases() {
        return Response.success(knowledgeBaseService.listAllKnowledgeBases());
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    public Response<KnowledgeBaseVO> getKnowledgeBase(@PathVariable String id) {
        return Response.success(knowledgeBaseService.getKnowledgeBaseById(id));
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Response<Void> deleteKnowledgeBase(@PathVariable String id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return Response.success();
    }

    /**
     * 上传文档到知识库
     */
    @PostMapping("/{id}/documents")
    public Response<String> uploadDocument(@PathVariable String id, @RequestParam("file") MultipartFile file) {
        String documentId = knowledgeBaseService.uploadDocument(id, file);
        return Response.success(documentId);
    }

    /**
     * 获取知识库下的文档列表（分页）
     */
    @GetMapping("/{id}/documents")
    public Response<?> listDocuments(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ocrStatus) {
        return Response.success(knowledgeBaseService.listDocumentsPage(id, name, status, ocrStatus, page, size));
    }

    /**
     * 对文档进行分块
     */
    @PostMapping("/documents/{documentId}/split")
    public Response<List<DocumentChunk>> splitDocument(
            @PathVariable String documentId,
            @RequestBody(required = false) SplitDocumentRequest request) {
        if (request != null) {
            List<DocumentChunk> chunks = knowledgeBaseService.splitDocument(
                    documentId, request.getStrategy(), request.getChunkSize(),
                    request.getOverlapRatio(), request.getDelimiter());
            return Response.success(chunks);
        } else {
            List<DocumentChunk> chunks = knowledgeBaseService.splitDocument(documentId, null, null, null);
            return Response.success(chunks);
        }
    }

    /**
     * 对文档进行向量化（先清理旧向量再重建）——异步：提交事件后立即返回
     */
    @PostMapping("/documents/{documentId}/embed")
    public Response<Void> embedDocument(@PathVariable String documentId) {
        eventPublisher.publish(BizEventType.DOCUMENT_EMBED, documentId, null);
        return Response.success();
    }

    /**
     * 批量向量化文档（每个文档先清理旧向量再重建）——异步：逐个发布事件
     */
    @PostMapping("/documents/batch-embed")
    public Response<Void> batchEmbedDocuments(@RequestBody List<String> documentIds) {
        for (String documentId : documentIds) {
            try {
                eventPublisher.publish(BizEventType.DOCUMENT_EMBED, documentId, null);
            } catch (Exception e) {
                log.error("批量向量化提交失败: documentId={}", documentId, e);
            }
        }
        return Response.success();
    }

    /**
     * 获取文档的所有分块
     */
    @GetMapping("/documents/{documentId}/chunks")
    public Response<List<DocumentChunk>> getDocumentChunks(@PathVariable String documentId) {
        return Response.success(knowledgeBaseService.getDocumentChunks(documentId));
    }

    /**
     * 获取文档原始内容
     */
    @GetMapping("/documents/{documentId}/content")
    public Response<String> getDocumentContent(@PathVariable String documentId) {
        KnowledgeDocument doc = knowledgeBaseService.getDocumentById(documentId);
        if(!CollectionUtil.isEmpty(doc.getPageContents())){
            StringJoiner stringJoiner=new StringJoiner("\n");
            doc.getPageContents().forEach(pageContent -> stringJoiner.add(pageContent.getText()));
            doc.setContent(stringJoiner.toString());
        }
        return Response.success(doc.getContent());
    }

    /**
     * 下载文档
     */
    @GetMapping("/documents/{docId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String docId) {
        Path filePath = knowledgeBaseService.getDocumentFile(docId);
        String fileName = knowledgeBaseService.getDocumentFileName(docId);

        FileSystemResource resource = new FileSystemResource(filePath.toFile());

        String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName;

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    /**
     * 在线预览文档（内联展示，支持PDF等）
     */
    @GetMapping("/documents/{docId}/view")
    public ResponseEntity<Resource> viewDocument(@PathVariable String docId) {
        Path filePath = knowledgeBaseService.getDocumentFile(docId);
        String fileName = knowledgeBaseService.getDocumentFileName(docId);

        FileSystemResource resource = new FileSystemResource(filePath.toFile());

        MediaType mediaType = getMediaType(fileName);

        String encodedFileName = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentDisposition = "inline; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    private MediaType getMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        } else if (lower.endsWith(".txt") || lower.endsWith(".md")) {
            return MediaType.TEXT_PLAIN;
        } else if (lower.endsWith(".docx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{docId}")
    public Response<Void> deleteDocument(@PathVariable String docId) {
        knowledgeBaseService.deleteDocument(docId);
        return Response.success();
    }

    /**
     * 批量删除文档
     */
    @DeleteMapping("/documents/batch")
    public Response<Void> deleteDocuments(@RequestBody List<String> documentIds) {
        knowledgeBaseService.deleteDocuments(documentIds);
        return Response.success();
    }

    /**
     * 批量分块文档（默认PARAGRAPH策略）
     */
    @PostMapping("/documents/batch-split")
    public Response<Void> batchSplitDocuments(@RequestBody List<String> documentIds) {
        knowledgeBaseService.batchSplitDocuments(documentIds);
        return Response.success();
    }

    /**
     * 知识库向量搜索
     */
    @PostMapping("/{id}/search")
    public Response<List<SearchResultVO>> searchKnowledgeBase(
            @PathVariable String id,
            @RequestBody java.util.Map<String, Object> request) {
        String query = (String) request.get("query");
        int topK = request.get("topK") != null ? (Integer) request.get("topK") : 3;
        return Response.success(knowledgeBaseService.searchVectors(id, query, topK));
    }

    // ========== 分块管理 ==========

    /**
     * 批量向量化分块
     */
    @PostMapping("/chunks/batch-embed")
    public Response<Void> batchEmbedChunks(@RequestBody java.util.Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> chunkIds = (List<String>) request.get("chunkIds");
        knowledgeBaseService.batchEmbedChunks(chunkIds);
        return Response.success();
    }

    /**
     * 删除分块向量数据
     */
    @PostMapping("/chunks/delete-vectors")
    public Response<Void> deleteChunkVectors(@RequestBody java.util.Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> chunkIds = (List<String>) request.get("chunkIds");
        knowledgeBaseService.deleteChunkVectors(chunkIds);
        return Response.success();
    }

    /**
     * 批量删除分块
     */
    @PostMapping("/chunks/batch-delete")
    public Response<Void> batchDeleteChunks(@RequestBody java.util.Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> chunkIds = (List<String>) request.get("chunkIds");
        knowledgeBaseService.batchDeleteChunks(chunkIds);
        return Response.success();
    }

    /**
     * 合并分块
     */
    @PostMapping("/chunks/merge")
    public Response<DocumentChunk> mergeChunks(@RequestBody java.util.Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> chunkIds = (List<String>) request.get("chunkIds");
        String documentId = (String) request.get("documentId");
        String knowledgeBaseId = (String) request.get("knowledgeBaseId");
        DocumentChunk merged = knowledgeBaseService.mergeChunks(chunkIds, documentId, knowledgeBaseId);
        return Response.success(merged);
    }

    // ========== 手动分块 ==========

    /**
     * 创建单个手动分块
     */
    @PostMapping("/chunks/manual")
    public Response<DocumentChunk> createManualChunk(@RequestBody java.util.Map<String, Object> request) {
        String documentId = (String) request.get("documentId");
        String content = (String) request.get("content");
        int sequence = request.get("sequence") instanceof Number ? ((Number) request.get("sequence")).intValue() : 0;
        DocumentChunk chunk = chunkService.createManualChunk(documentId, content, sequence);
        return Response.success(chunk);
    }

    /**
     * 更新分块内容
     */
    @PutMapping("/chunks/{chunkId}")
    public Response<DocumentChunk> updateChunkContent(@PathVariable String chunkId,
                                                      @RequestBody java.util.Map<String, String> request) {
        String content = request.get("content");
        DocumentChunk chunk = chunkService.updateChunkContent(chunkId, content);
        return Response.success(chunk);
    }

    /**
     * 批量更新分块序号（拖拽排序）
     */
    @PutMapping("/chunks/reorder")
    public Response<Void> reorderChunks(@RequestBody java.util.List<java.util.Map<String, Object>> orderList) {
        chunkService.reorderChunks(orderList);
        return Response.success();
    }

    /**
     * 批量保存手动分块（清空旧分块后创建新分块）
     */
    @PostMapping("/chunks/manual-batch")
    public Response<java.util.List<DocumentChunk>> saveManualChunks(@RequestBody java.util.Map<String, Object> request) {
        String documentId = (String) request.get("documentId");
        String knowledgeBaseId = (String) request.get("knowledgeBaseId");
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> chunks = (java.util.List<java.util.Map<String, Object>>) request.get("chunks");
        java.util.List<DocumentChunk> result = chunkService.saveManualChunks(documentId, knowledgeBaseId, chunks);
        return Response.success(result);
    }

    // ========== OCR ==========

    /**
     * 查询文档OCR状态（供前端轮询）
     */
    @GetMapping("/documents/{documentId}/ocr-status")
    public Response<String> ocrStatus(@PathVariable String documentId) {
        com.agentscope.model.entity.KnowledgeDocument doc = knowledgeBaseService.getDocumentById(documentId);
        if (doc == null) {
            return Response.error(404, "文档不存在");
        }
        return Response.success(doc.getOcrStatus() != null ? doc.getOcrStatus().getCode() : "NONE");
    }

    /**
     * OCR 整个文档（所有页面）——异步：提交事件后立即返回，前端轮询ocrStatus
     */
    @PostMapping("/documents/{documentId}/ocr")
    public Response<Void> ocrDocument(@PathVariable String documentId) {
        com.agentscope.model.entity.KnowledgeDocument doc = knowledgeBaseService.getDocumentById(documentId);
        if (doc == null) {
            return Response.error(404, "文档不存在");
        }
        doc.setOcrStatus(OcrStatus.PROCESSING);
        knowledgeBaseService.updateDocument(doc);
        eventPublisher.publish(BizEventType.DOCUMENT_OCR, documentId, null);
        return Response.success();
    }

    /**
     * OCR 文档指定页
     */
    @PostMapping("/documents/{documentId}/ocr/{pageIndex}")
    public Response<KnowledgeDocument.PageContent> ocrPage(
            @PathVariable String documentId, @PathVariable int pageIndex) {
        com.agentscope.model.entity.KnowledgeDocument doc = knowledgeBaseService.getDocumentById(documentId);
        if (doc == null) {
            return Response.error(404, "文档不存在");
        }
        KnowledgeDocument.PageContent pageContent = ocrService.ocrSinglePage(doc, pageIndex - 1);
        if (doc.getPageContents() == null) {
            doc.setPageContents(new java.util.ArrayList<>());
        }
        doc.getPageContents().removeIf(p -> p.getPage() == pageContent.getPage());
        doc.getPageContents().add(pageContent);
        doc.getPageContents().sort((a, b) -> Integer.compare(a.getPage(), b.getPage()));

        // 判断OCR状态：获取总页数，检查是否全部完成
        try {
            int totalPages = com.agentscope.util.PdfImageUtil.getPageCount(
                    java.nio.file.Path.of(doc.getFilePath()));
            long ocrCount = doc.getPageContents().stream()
                    .filter(p -> p.getText() != null && !p.getText().isEmpty())
                    .count();
            doc.setOcrStatus(ocrCount >= totalPages ? OcrStatus.DONE : OcrStatus.PART);
        } catch (Exception e) {
            doc.setOcrStatus(OcrStatus.PART);
        }

        knowledgeBaseService.updateDocument(doc);
        return Response.success(pageContent);
    }

    /**
     * 获取文档的页面内容（用于朗读）
     */
    @GetMapping("/documents/{documentId}/pages")
    public Response<List<KnowledgeDocument.PageContent>> getDocumentPages(@PathVariable String documentId) {
        com.agentscope.model.entity.KnowledgeDocument doc = knowledgeBaseService.getDocumentById(documentId);
        if (doc == null) {
            return Response.error(404, "文档不存在");
        }
        return Response.success(doc.getPageContents());
    }

    /**
     * 获取PDF文档总页数
     */
    @GetMapping("/documents/{documentId}/page-count")
    public Response<Integer> getDocumentPageCount(@PathVariable String documentId) {
        com.agentscope.model.entity.KnowledgeDocument doc = knowledgeBaseService.getDocumentById(documentId);
        if (doc == null) {
            return Response.error(404, "文档不存在");
        }
        try {
            int pageCount = PdfImageUtil.getPageCount(xFileStorage.resolve(doc.getFilePath()));
            return Response.success(pageCount);
        } catch (Exception e) {
            log.error("获取PDF页数失败: {}", documentId, e);
            return Response.error(500, "获取页数失败");
        }
    }
}
