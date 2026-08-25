package com.agentscope.model.vo;

import com.agentscope.common.enums.DocumentStatus;
import com.agentscope.common.enums.DocumentType;
import com.agentscope.common.enums.OcrStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVO {

    /** 文档ID */
    private String id;

    /** 文档名称 */
    private String name;

    /** 文档类型 */
    private DocumentType type;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 所属知识库ID */
    private String knowledgeBaseId;

    /** 分块数量 */
    private Integer chunkCount;

    /** 已向量化的分块数量 */
    private Integer embeddedCount;

    /** 文档状态 */
    private DocumentStatus status;

    /** OCR状态 */
    private OcrStatus ocrStatus;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
