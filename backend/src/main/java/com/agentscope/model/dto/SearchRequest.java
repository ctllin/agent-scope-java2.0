package com.agentscope.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库搜索请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /** 搜索内容 */
    @NotBlank(message = "搜索内容不能为空")
    private String query;

    /** 返回结果数量，默认5 */
    private Integer topK = 5;
}
