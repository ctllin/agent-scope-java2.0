package com.agentscope.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 文档分块实体
 * 存储在 MongoDB 的 document_chunks 集合中
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "document_chunks")
public class DocumentChunk {

    @Id
    private String id;

    /** 所属文档ID */
    private String documentId;

    /** 所属文档名称（冗余，用于前端展示） */
    private String documentName;

    /** 所属知识库ID */
    private String knowledgeBaseId;

    /** 分块文本内容 */
    private String content;

    /** 分块在原文档中的序号（从0开始） */
    private Integer sequence;

    /** 分割策略：paragraph / characters / manual / merged */
    private String splitStrategy;

    /** 是否已完成Embedding向量化 */
    @Builder.Default
    private Boolean embedded = false;

    /** Milvus向量ID，用于精确删除 */
    private String vectorId;

    /** 逻辑删除标记：NOT_DELETED / DELETED */
    @Builder.Default
    private String delFlag = "NOT_DELETED";

    /** 分块创建时间 */
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();
}
