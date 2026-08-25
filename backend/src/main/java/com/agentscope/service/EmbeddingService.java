package com.agentscope.service;

import java.util.List;

/**
 * Embedding服务接口
 * <p>
 * 提供文本向量化功能
 * </p>
 */
public interface EmbeddingService {

    /**
     * 将单个文本转换为向量
     *
     * @param text 输入文本
     * @return 向量数组
     */
    float[] embed(String text);

    /**
     * 将多个文本转换为向量
     *
     * @param texts 输入文本列表
     * @return 向量列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 获取向量维度
     *
     * @return 向量维度
     */
    int getDimension();
}
