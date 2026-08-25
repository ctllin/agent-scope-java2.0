package com.agentscope.model.entity;

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
 * 语音合成记录：一篇待朗读文本 + 分段信息 + 拼接后的完整音频
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tts_records")
public class TtsRecord {

    /** 合成模式：按行 */
    public static final String MODE_LINE = "LINE";
    /** 合成模式：按段落 */
    public static final String MODE_PARAGRAPH = "PARAGRAPH";
    /** 合成模式：整篇 */
    public static final String MODE_ALL = "ALL";

    /** 状态：合成中 */
    public static final String STATUS_SYNTHESIZING = "SYNTHESIZING";
    /** 状态：完成 */
    public static final String STATUS_DONE = "DONE";
    /** 状态：失败 */
    public static final String STATUS_FAILED = "FAILED";

    @Id
    private String id;

    /** 标题（为空时自动取正文前20字） */
    private String title;

    /** 原始全文（高亮渲染依据） */
    private String text;

    /** 合成模式：LINE/PARAGRAPH/ALL */
    private String mode;

    /** 状态：SYNTHESIZING/DONE/FAILED */
    private String status;

    /** 失败原因 */
    private String errorMessage;

    /** 分段信息（含原文偏移与各段时长，供播放高亮） */
    private List<TtsSegment> segments;

    /** 拼接后的完整音频路径 */
    private String audioPath;

    /** 音频总时长（秒） */
    private Double duration;

    /** 音频文件大小（字节） */
    private Long fileSize;

    /** 全文字符数 */
    private Integer charCount;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
