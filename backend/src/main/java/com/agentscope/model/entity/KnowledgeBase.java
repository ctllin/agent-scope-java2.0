package com.agentscope.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 知识库实体类
 * <p>
 * 存储知识库基本信息
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "knowledge_bases")
public class KnowledgeBase {

    /** 知识库ID */
    @Id
    private String id;

    /** 知识库名称（如：运维、开发、产品） */
    private String name;

    /** 知识库描述 */
    private String description;

    /** 知识库图标 */
    private String icon;

    /** 文档数量 */
    private Integer documentCount;

    /** 向量数量 */
    private Integer vectorCount;

    /** 创建者ID */
    private String creatorId;

    /** 创建时间 */
    @CreatedDate
    private LocalDateTime createdAt;

    /** 更新时间 */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
