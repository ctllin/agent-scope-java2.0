package com.agentscope.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建知识库请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeBaseRequest {

    /** 知识库名称 */
    @NotBlank(message = "知识库名称不能为空")
    private String name;

    /** 知识库描述 */
    private String description;

    /** 知识库图标 */
    private String icon;
}
