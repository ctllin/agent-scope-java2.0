package com.agentscope.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OCR状态枚举
 */
@Getter
@AllArgsConstructor
public enum OcrStatus {

    NONE("NONE", "未OCR"),
    PART("PART", "部分OCR"),
    DONE("DONE", "已OCR"),
    PROCESSING("PROCESSING", "识别中"),
    FAILED("FAILED", "识别失败");

    private final String code;
    private final String description;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static OcrStatus fromCode(String code) {
        for (OcrStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
