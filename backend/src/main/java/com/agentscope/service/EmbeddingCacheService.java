package com.agentscope.service;

import java.util.List;

/**
 * 向量缓存服务。
 * <p>
 * 以文本MD5为键的进程内向量缓存（Caffeine），
 * 避免相同分块内容重复调用embedding模型。
 */
public interface EmbeddingCacheService {

    /** 从缓存获取文本向量，未命中返回null */
    float[] get(String text);

    /** 写入缓存 */
    void put(String text, float[] vector);

    /** 批量获取：命中部分返回非null元素占位，顺序与入参一致 */
    List<float[]> getBatch(List<String> texts);
}
