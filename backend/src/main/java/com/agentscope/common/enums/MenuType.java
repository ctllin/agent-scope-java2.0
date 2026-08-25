package com.agentscope.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单类型枚举
 */
@Getter
@AllArgsConstructor
public enum MenuType {

    /** 目录 */
    DIRECTORY(0, "目录"),
    
    /** 菜单 */
    MENU(1, "菜单"),
    
    /** 按钮 */
    BUTTON(2, "按钮");

    private final int code;
    private final String description;

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static MenuType fromCode(int code) {
        for (MenuType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的菜单类型: " + code);
    }
}
