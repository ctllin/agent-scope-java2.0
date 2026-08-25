package com.agentscope.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文档类型枚举
 */
@Getter
@AllArgsConstructor
public enum DocumentType {

    /** PDF文档 */
    PDF("pdf", "PDF文档"),
    
    /** Word文档 */
    DOCX("docx", "Word文档"),
    
    /** 文本文件 */
    TXT("txt", "文本文件");

    private final String code;
    private final String description;

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static DocumentType fromCode(String code) {
        for (DocumentType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据扩展名获取文档类型
     * @param extension 文件扩展名
     * @return 对应的文档类型，如果不支持则返回null
     */
    public static DocumentType fromExtension(String extension) {
        return fromCode(extension);
    }
}
