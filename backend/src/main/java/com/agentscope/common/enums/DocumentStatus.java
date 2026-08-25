package com.agentscope.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档状态枚举
 */
@Getter
@AllArgsConstructor
public enum DocumentStatus {

    UPLOADED("UPLOADED", "已上传"),
    SPLIT("SPLIT", "已分块");

    private final String code;
    private final String description;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static DocumentStatus fromCode(String code) {
        for (DocumentStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
