package com.agentscope.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档分块策略枚举
 */
@Getter
@AllArgsConstructor
public enum SplitStrategy {

    /** 自动选择（仅用于入参，不持久化到分块） */
    AUTO("auto", "自动选择"),
    /** 段落分割 */
    PARAGRAPH("paragraph", "段落分割"),
    /** 字符分割 */
    CHARACTERS("characters", "字符分割"),
    /** 手动分块 */
    MANUAL("manual", "手动分块"),
    /** 合并分块 */
    MERGED("merged", "合并分块");

    private final String code;
    private final String description;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static SplitStrategy fromCode(String code) {
        for (SplitStrategy strategy : values()) {
            if (strategy.code.equalsIgnoreCase(code)) {
                return strategy;
            }
        }
        return null;
    }
}
