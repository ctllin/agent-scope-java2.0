package com.agentscope.controller;

import cn.hutool.core.util.StrUtil;
import com.agentscope.common.Response;
import com.agentscope.model.entity.TtsRecord;
import com.agentscope.service.XFileStorageService;
import com.agentscope.service.TtsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 语音合成控制器
 */
@RestController
@RequestMapping("/api/tts-records")
@RequiredArgsConstructor
public class TtsRecordController {

    private final TtsRecordService ttsRecordService;
    private final XFileStorageService xFileStorage;

    /**
     * 创建合成任务（异步合成，返回SYNTHESIZING状态记录，前端轮询）
     *
     * @param body {title?, text, mode: LINE/PARAGRAPH/ALL}
     */
    @PostMapping
    public Response<TtsRecord> create(@RequestBody Map<String, String> body) {
        return Response.success(ttsRecordService.create(
                body.get("title"),
                body.get("text"),
                body.get("mode")));
    }

    /**
     * 分页查询（status: SYNTHESIZING/DONE/FAILED；keyword: 标题/内容模糊搜索）
     */
    @GetMapping
    public Response<Map<String, Object>> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        List<TtsRecord> records = ttsRecordService.listRecords(status, keyword, page, size);
        long total = ttsRecordService.countRecords(status, keyword);
        return Response.success(Map.of(
                "records", records,
                "total", total,
                "page", page,
                "size", size
        ));
    }

    /**
     * 记录详情（含分段偏移，用于高亮播放页）
     */
    @GetMapping("/{id}")
    public Response<TtsRecord> getRecord(@PathVariable String id) {
        return Response.success(ttsRecordService.getRecord(id));
    }

    /**
     * 删除记录及音频文件
     */
    @DeleteMapping("/{id}")
    public Response<Void> deleteRecord(@PathVariable String id) {
        ttsRecordService.deleteRecord(id);
        return Response.success();
    }

    /**
     * 批量删除记录及音频文件
     */
    @PostMapping("/batch-delete")
    public Response<Void> deleteRecords(@RequestBody List<String> recordIds) {
        ttsRecordService.deleteRecords(recordIds);
        return Response.success();
    }

    /**
     * 播放/下载拼接后的完整音频
     */
    @GetMapping("/{id}/audio")
    public ResponseEntity<FileSystemResource> audio(@PathVariable String id) {
        TtsRecord record = ttsRecordService.getRecord(id);
        if (StrUtil.isBlank(record.getAudioPath())) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(
                xFileStorage.resolve(record.getAudioPath()).toFile());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .body(resource);
    }
}
