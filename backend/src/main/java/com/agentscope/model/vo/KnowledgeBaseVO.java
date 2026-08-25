package com.agentscope.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库信息视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseVO {

    /** 知识库ID */
    private String id;

    /** 知识库名称 */
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
    private LocalDateTime createdAt;
}
