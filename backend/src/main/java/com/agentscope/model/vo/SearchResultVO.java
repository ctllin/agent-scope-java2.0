package com.agentscope.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库搜索结果VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultVO {

    /** 文本内容 */
    private String content;

    /** 相似度分数（0-1） */
    private Float score;

    /** 来源文档ID */
    private String documentId;

    /** 来源文档名称 */
    private String documentName;

    /** 知识库ID */
    private String knowledgeBaseId;
}
