package com.agentscope.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * x-file-storage 文件登记记录
 * <p>
 * 记录通过统一存储上传的文件（URL、平台、业务归属），
 * 删除记录时可据此删除物理文件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "xfile_records")
public class XFileRecord {

    @Id
    private String id;

    /** 文件访问URL */
    private String url;

    /** 文件名（含路径，x-file-storage按此检索） */
    private String filename;

    /** 原始文件名 */
    private String originalFilename;

    /** 文件大小（字节） */
    private Long size;

    /** 存储平台标识 */
    private String platform;

    /** 存储相对路径key（业务表里保存的值） */
    private String storageKey;

    /** 业务模块：doc/asr/asr-rt/tts */
    private String module;

    /** 关联业务ID（kbId/recordId等） */
    private String bizId;

    /** FileInfo完整JSON（便于追溯与扩展） */
    private String fileInfoJson;

    @CreatedDate
    private LocalDateTime createdAt;
}
