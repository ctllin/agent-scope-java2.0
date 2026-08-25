package com.agentscope.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI模型类型枚举
 */
@Getter
@AllArgsConstructor
public enum AiModelType {

    /** GLM-4-Flash */
    GLM_4_FLASH("glm-4-flash", "GLM-4-Flash模型"),
    
    /** GLM-5 */
    GLM_5("glm-5", "GLM-5模型");

    private final String code;
    private final String description;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static AiModelType fromCode(String code) {
        for (AiModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的模型类型: " + code);
    }
}
