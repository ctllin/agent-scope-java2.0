package com.agentscope.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Embedding向量缓存实体
 * 存储文本到向量的映射，避免重复计算
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "embedding_cache")
public class EmbeddingCache {

    @Id
    private String id;

    /** 文本内容的MD5哈希（作为查询索引） */
    private String textHash;

    /** 原始文本内容 */
    private String text;

    /** 向量数据（JSON数组） */
    private String vectorJson;

    /** 向量维度 */
    private int dimension;

    /** 创建时间 */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
