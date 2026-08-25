package com.agentscope.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对话模式枚举
 */
@Getter
@AllArgsConstructor
public enum ChatMode {

    /** 普通对话 */
    NORMAL("normal", "普通对话"),
    
    /** 知识库对话 */
    KNOWLEDGE_BASE("knowledge_base", "知识库对话");

    private final String code;
    private final String description;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static ChatMode fromCode(String code) {
        for (ChatMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("未知的对话模式: " + code);
    }
}
