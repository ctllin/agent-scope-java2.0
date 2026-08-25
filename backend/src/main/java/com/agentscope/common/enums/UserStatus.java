package com.agentscope.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 * <p>
 * JSON序列化为数字code（1启用/0禁用），与前端约定一致
 * </p>
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    /** 启用 */
    ENABLED(1, "启用"),

    /** 禁用 */
    DISABLED(0, "禁用");

    private final int code;
    private final String description;

    @JsonValue
    public int toJson() {
        return code;
    }

    @JsonCreator
    public static UserStatus fromCode(int code) {
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知用户状态码: " + code);
    }
}
