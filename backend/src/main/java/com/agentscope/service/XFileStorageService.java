package com.agentscope.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 统一文件存储服务（基于 x-file-storage 本地模式）。
 * <p>
 * 项目内所有业务资产（文档原件 doc / 语音上传 asr / 实时录音 asr-rt / 合成音频 tts）
 * 的落盘、路径解析与物理删除唯一入口，业务代码禁止直接操作磁盘路径。
 * 业务表保存相对存储key；删除记录时必须调用 {@link #delete(String)} 同步清理物理文件。
 * 历史绝对路径自动兼容透传。
 */
public interface XFileStorageService {

    /** 存储上传文件，返回存储key（如 doc/{kbId}/{uuid}.pdf） */
    String store(MultipartFile file, String module, String bizId) throws IOException;

    /** 存储内存字节；saveFilename传固定名即覆盖语义 */
    String storeBytes(byte[] bytes, String module, String bizId, String saveFilename);

    /**
     * 删除文件：优先经x-file-storage按URL删除，再做磁盘兜底清理并移除登记记录；
     * 历史绝对路径直接删磁盘。失败仅记录日志不抛出。
     */
    void delete(String keyOrPath);

    /** 解析为绝对磁盘路径（本地模式），供流式输出/ffmpeg等本地工具使用 */
    Path resolve(String keyOrPath);

    /** 是否为旧版绝对路径（以/开头或含Windows盘符） */
    boolean isLegacyAbsolute(String path);

    // ==================== 知识库模块旧签名兼容 ====================

    /** 存储知识库文档原件（module=doc） */
    String storeFile(MultipartFile file, String knowledgeBaseId) throws IOException;

    /** 解析文件绝对路径（旧签名兼容） */
    Path getFilePath(String keyOrPath);

    /** 删除单个文件（旧签名兼容） */
    void deleteFile(String keyOrPath);

    /** 删除知识库目录下所有文档文件（新旧布局均覆盖） */
    void deleteKnowledgeBaseFiles(String knowledgeBaseId);
}
