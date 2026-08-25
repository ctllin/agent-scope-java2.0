package com.agentscope.model.entity;

import com.agentscope.common.enums.DocumentStatus;
import com.agentscope.common.enums.DocumentType;
import com.agentscope.common.enums.OcrStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档实体类
 * 存储上传文档的基本信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
public class KnowledgeDocument {

    @Id
    private String id;

    /** 文档名称 */
    private String name;

    /** 文档类型（pdf、docx、txt） */
    private DocumentType type;

    /** 文件路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文档内容（纯文本） */
    private String content;

    /** 每页内容（PDF/Word等分页文档），格式：[{page:1, text:"...", imageUrl:"..."}] */
    private List<PageContent> pageContents;

    /** 所属知识库ID */
    private String knowledgeBaseId;

    /** 分块数量 */
    @Builder.Default
    private Integer chunkCount = 0;

    /** 已向量化的分块数量 */
    @Builder.Default
    private Integer embeddedCount = 0;

    /** 文档状态 */
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;
    /** ocr状态 */
    private OcrStatus ocrStatus = OcrStatus.NONE;

    /** 创建者ID */
    private String creatorId;

    /** 创建时间 */
    @CreatedDate
    private LocalDateTime createdAt;

    /** 更新时间 */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageContent {
        /** 页码（从1开始） */
        private int page;
        /** 该页OCR提取的文本 */
        private String text;
        /** 该页渲染的图片路径（用于OCR和展示） */
        private String imageUrl;
    }
}
