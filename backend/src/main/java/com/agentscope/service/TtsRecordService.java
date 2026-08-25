package com.agentscope.service;

import com.agentscope.model.entity.TtsRecord;

import java.util.List;

/**
 * 语音合成记录服务
 */
public interface TtsRecordService {

    /**
     * 创建合成任务：切分文本并异步合成，立即返回记录（状态SYNTHESIZING），前端轮询
     *
     * @param title 标题，为空自动取正文前20字
     * @param text  全文
     * @param mode  LINE/PARAGRAPH/ALL
     */
    TtsRecord create(String title, String text, String mode);

    /** 同步执行合成任务（由事件消费者调用） */
    void synthesizeNow(String recordId);

    /** 分页查询（status: SYNTHESIZING/DONE/FAILED；keyword: 标题/内容模糊） */
    List<TtsRecord> listRecords(String status, String keyword, int page, int size);

    long countRecords(String status, String keyword);

    TtsRecord getRecord(String recordId);

    /** 删除记录及音频目录 */
    void deleteRecord(String recordId);

    void deleteRecords(List<String> recordIds);
}
