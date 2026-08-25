package com.agentscope.service;

import com.agentscope.common.enums.SplitStrategy;
import com.agentscope.model.dto.CreateKnowledgeBaseRequest;
import com.agentscope.model.entity.DocumentChunk;
import com.agentscope.model.vo.DocumentVO;
import com.agentscope.model.vo.KnowledgeBaseVO;
import com.agentscope.model.vo.SearchResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /** 创建知识库 */
    KnowledgeBaseVO createKnowledgeBase(CreateKnowledgeBaseRequest request);

    /** 删除知识库 */
    void deleteKnowledgeBase(String id);

    /** 获取所有知识库 */
    List<KnowledgeBaseVO> listAllKnowledgeBases();

    /** 根据ID获取知识库 */
    KnowledgeBaseVO getKnowledgeBaseById(String id);

    /** 上传文档到知识库（不自动分块） */
    String uploadDocument(String knowledgeBaseId, MultipartFile file);

    /** 对文档进行分块处理（清理旧分块和向量） */
    List<DocumentChunk> splitDocument(String documentId, SplitStrategy strategy,
                                       Integer chunkSize, Double overlapRatio);

    /** 对文档进行分块处理（支持手动分块分隔符） */
    List<DocumentChunk> splitDocument(String documentId, SplitStrategy strategy,
                                       Integer chunkSize, Double overlapRatio, String delimiter);

    /** 批量分块文档（默认PARAGRAPH策略） */
    void batchSplitDocuments(List<String> documentIds);

    /** 获取文档的所有分块 */
    List<DocumentChunk> getDocumentChunks(String documentId);

    /** 获取知识库下的文档列表 */
    List<DocumentVO> listDocuments(String knowledgeBaseId);

    /** 分页查询文档列表 */
    Map<String, Object> listDocumentsPage(String knowledgeBaseId, String name, String status, String ocrStatus, int page, int size);

    /** 删除文档（清理分块和向量） */
    void deleteDocument(String documentId);

    /** 批量删除文档（清理分块+向量+磁盘文件） */
    void deleteDocuments(List<String> documentIds);

    /** 获取文档文件路径 */
    Path getDocumentFile(String documentId);

    /** 获取文档原始文件名 */
    String getDocumentFileName(String documentId);

    /** 获取文档实体 */
    com.agentscope.model.entity.KnowledgeDocument getDocumentById(String documentId);

    /** 更新文档 */
    void updateDocument(com.agentscope.model.entity.KnowledgeDocument document);

    /** 知识库向量搜索 */
    List<SearchResultVO> searchVectors(String knowledgeBaseId, String query, int topK);

    /** 批量embedding分块 */
    void batchEmbedChunks(List<String> chunkIds);

    /** 重新向量化文档（先清理该文档全部旧向量，再全量重建） */
    void reembedDocument(String documentId);

    /** 删除分块的向量数据 */
    void deleteChunkVectors(List<String> chunkIds);

    /** 批量删除分块（含向量清理） */
    void batchDeleteChunks(List<String> chunkIds);

    /** 合并分块（含向量清理） */
    DocumentChunk mergeChunks(List<String> chunkIds, String documentId, String knowledgeBaseId);
}
