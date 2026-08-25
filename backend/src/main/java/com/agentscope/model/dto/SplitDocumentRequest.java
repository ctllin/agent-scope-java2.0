package com.agentscope.model.dto;

import com.agentscope.common.enums.SplitStrategy;
import lombok.Data;

/**
 * 文档分块请求 DTO
 */
@Data
public class SplitDocumentRequest {

    /** 分块策略 */
    private SplitStrategy strategy = SplitStrategy.AUTO;

    /** 分块大小（字符数），不传则使用配置中的默认值 */
    private Integer chunkSize;

    /** 重叠比例（0-1），不传则使用配置中的默认值 */
    private Double overlapRatio;

    /** 手动分块时的分隔符（正则表达式） */
    private String delimiter;
}
