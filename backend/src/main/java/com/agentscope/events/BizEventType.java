package com.agentscope.events;

/**
 * 业务事件类型（耗时任务）
 */
public enum BizEventType {
    /** 文档OCR识别 */
    DOCUMENT_OCR,
    /** 文档向量化 */
    DOCUMENT_EMBED,
    /** 语音识别（文件） */
    ASR_RECOGNIZE,
    /** 语音合成 */
    TTS_SYNTHESIZE
}
