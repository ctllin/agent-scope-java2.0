package com.agentscope.service;

import com.agentscope.model.entity.AsrRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 语音识别服务
 */
public interface AsrService {

    /** 上传音频文件（支持批量） */
    List<AsrRecord> uploadAudios(List<MultipartFile> files);

    /** 分页查询记录（source/status/keyword为null查全部，keyword按识别内容模糊匹配） */
    List<AsrRecord> listRecords(String source, String status, String keyword, int page, int size);

    long countRecords(String source, String status, String keyword);

    /** 发布识别事件（重复识别覆盖旧结果）；lang: cn/en，默认cn */
    AsrRecord recognize(String recordId, String lang);

    /** 同步执行识别任务（由事件消费者调用） */
    void recognizeNow(String recordId, String lang);

    /** 批量异步识别 */
    void recognizeBatch(List<String> recordIds, String lang);

    /** 删除记录及关联文件 */
    void deleteRecord(String recordId);

    /** 批量删除记录及关联文件 */
    void deleteRecords(List<String> recordIds);

    /** 按ID查询记录 */
    AsrRecord getRecord(String recordId);

    /** 保存实时识别结果（覆盖同会话旧结果由前端传recordId实现），lang: cn/en */
    AsrRecord saveRealtimeResult(String recordId, String name, String text, Long durationSeconds, String lang);

    /** 为实时识别记录挂载录音文件（16k单声道16bit WAV字节） */
    void attachAudio(String recordId, byte[] wavBytes);
}
