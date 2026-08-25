package com.agentscope.service.document;

import com.agentscope.model.entity.DocumentChunk;
import com.agentscope.common.enums.SplitStrategy;

import java.util.List;
import java.util.Map;

/**
 * 文档分块服务：内容切分与分块记录的增删查。
 */
public interface ChunkService {

    /** 按默认策略切分内容并保存分块 */
    List<DocumentChunk> splitContent(String documentId, String knowledgeBaseId, String content);

    /** 按指定策略/块大小/重叠比例切分 */
    List<DocumentChunk> splitContent(String documentId, String knowledgeBaseId, String content,
                                     SplitStrategy strategy, Integer chunkSize, Double overlapRatio);

    /** 手动分隔符切分 */
    List<DocumentChunk> splitContent(String documentId, String knowledgeBaseId, String content,
                                     SplitStrategy strategy, Integer chunkSize, Double overlapRatio,
                                     String delimiter);

    /** 按文档查询全部分块 */
    List<DocumentChunk> getByDocumentId(String documentId);

    /** 按知识库查询全部分块 */
    List<DocumentChunk> getByKnowledgeBaseId(String knowledgeBaseId);

    /** 按ID查询分块 */
    DocumentChunk getById(String chunkId);

    /** 删除单个分块 */
    void deleteById(String chunkId);

    /** 批量删除分块 */
    void batchDelete(List<String> chunkIds);

    /** 清空文档全部分块 */
    void clearByDocumentId(String documentId);

    /** 合并多个分块为一个（内容拼接，向量标记重置） */
    DocumentChunk mergeChunks(List<String> chunkIds, String documentId, String knowledgeBaseId);

    /** 创建手动分块 */
    DocumentChunk createManualChunk(String documentId, String content, int sequence);

    /** 更新分块文本内容（向量化标记重置） */
    DocumentChunk updateChunkContent(String chunkId, String content);

    /** 按给定顺序重排分块sequence */
    void reorderChunks(List<Map<String, Object>> orderList);

    /** 保存手动分块全集（清空旧分块后重建） */
    List<DocumentChunk> saveManualChunks(String documentId, String knowledgeBaseId,
                                         List<Map<String, Object>> chunkDataList);
}
