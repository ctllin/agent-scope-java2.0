package com.agentscope.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 检索配置
 * 控制文档分割参数、向量检索 Top-K 数量和相似度阈值
 */
@Configuration
@ConfigurationProperties(prefix = "rag")
@Getter
@Setter
public class RagConfig {

    /** 分块最大字符数 */
    private int chunkSize = 500;

    /** 分块重叠比例（0.15 表示相邻分块重叠 15% 的内容） */
    private double overlapRatio = 0.15;

    /** 向量检索返回的 Top-K 结果数量 */
    private int topK = 3;

    /** 相似度阈值，低于此值的结果将被过滤 */
    private double similarityThreshold = 0.5;

    /** 是否保留文档结构（段落边界等） */
    private boolean structurePreserve = true;
}
