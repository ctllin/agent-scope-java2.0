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

/**
 * 语音识别记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "asr_records")
public class AsrRecord {

    /** 识别来源：文件上传 */
    public static final String SOURCE_FILE = "FILE";
    /** 识别来源：实时录音 */
    public static final String SOURCE_REALTIME = "REALTIME";

    /** 状态：已上传待识别 */
    public static final String STATUS_UPLOADED = "UPLOADED";
    /** 状态：识别中 */
    public static final String STATUS_RECOGNIZING = "RECOGNIZING";
    /** 状态：完成 */
    public static final String STATUS_DONE = "DONE";
    /** 状态：失败 */
    public static final String STATUS_FAILED = "FAILED";

    @Id
    private String id;

    /** 显示名称 */
    private String name;

    /** 原始音频文件路径（服务器存储） */
    private String filePath;

    /** 转码后的16k单声道WAV路径（识别用，可能为null表示无需转码或转码未执行） */
    private String wavPath;

    /** 文件大小（字节），实时录音为累计字节数 */
    private Long fileSize;

    /** 识别结果文本 */
    private String text;

    /** 状态：UPLOADED/RECOGNIZING/DONE/FAILED */
    private String status;

    /** 失败原因 */
    private String errorMessage;

    /** 来源：FILE/REALTIME */
    private String source;

    /** 识别语言：cn/en（默认cn） */
    private String language;

    /** 音频时长（秒，可空） */
    private Double duration;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
