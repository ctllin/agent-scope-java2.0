package com.agentscope.service;

import java.util.List;
import java.util.Map;

/**
 * Milvus服务接口
 * <p>
 * 提供Milvus向量数据库的操作方法
 * </p>
 */
public interface MilvusService {

    /**
     * 创建Collection
     *
     * @param collectionName Collection名称
     * @param dimension      向量维度
     */
    void createCollection(String collectionName, int dimension);

    /**
     * 删除Collection
     *
     * @param collectionName Collection名称
     */
    void dropCollection(String collectionName);

    /**
     * 检查Collection是否存在
     *
     * @param collectionName Collection名称
     * @return 是否存在
     */
    boolean hasCollection(String collectionName);

    /**
     * 获取Collection中的向量数量
     *
     * @param collectionName Collection名称
     * @return 向量数量
     */
    long getVectorCount(String collectionName);

    /**
     * 插入向量数据
     *
     * @param collectionName  Collection名称
     * @param knowledgeBaseId 知识库ID
     * @param documentId      文档ID
     * @param contents        文本内容列表
     * @param embeddings      向量列表
     */
    void insertVectors(String collectionName, String knowledgeBaseId, String documentId,
                       List<String> contents, List<float[]> embeddings);

    /**
     * 插入向量数据（带分块ID）
     *
     * @param collectionName  Collection名称
     * @param knowledgeBaseId 知识库ID
     * @param documentId      文档ID
     * @param chunkId         分块ID
     * @param contents        文本内容列表
     * @param embeddings      向量列表
     */
    void insertVectors(String collectionName, String knowledgeBaseId, String documentId,
                       String chunkId, List<String> contents, List<float[]> embeddings);

    /**
     * 相似性搜索
     *
     * @param collectionName Collection名称
     * @param queryVector    查询向量
     * @param topK           返回结果数量
     * @param filter         过滤表达式（如 knowledge_base_id = 'xxx'）
     * @return 搜索结果列表，每个元素包含 content 和 score
     */
    List<Map<String, Object>> search(String collectionName, float[] queryVector, int topK, String filter);

    /**
     * 按知识库ID删除所有向量
     *
     * @param collectionName  Collection名称
     * @param knowledgeBaseId 知识库ID
     */
    void deleteByKnowledgeBaseId(String collectionName, String knowledgeBaseId);

    /**
     * 按文档ID删除所有向量
     *
     * @param collectionName Collection名称
     * @param documentId     文档ID
     */
    void deleteByDocumentId(String collectionName, String documentId);

    /**
     * 按分块ID列表删除向量
     *
     * @param collectionName Collection名称
     * @param chunkIds       分块ID列表
     */
    void deleteByChunkIds(String collectionName, List<String> chunkIds);
}
