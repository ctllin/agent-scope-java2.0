package com.agentscope.controller;

import com.agentscope.common.Response;
import com.agentscope.model.entity.AsrRecord;
import com.agentscope.service.AsrService;
import com.agentscope.service.XFileStorageService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 语音识别控制器
 */
@RestController
@RequestMapping("/api/asr")
@RequiredArgsConstructor
public class AsrController {

    private final AsrService asrService;
    private final XFileStorageService xFileStorage;

    /**
     * 批量上传音频文件
     */
    @PostMapping("/upload")
    public Response<List<AsrRecord>> upload(@RequestParam("files") List<MultipartFile> files) {
        return Response.success(asrService.uploadAudios(files));
    }

    /**
     * 分页查询记录（source: FILE/REALTIME；status: UPLOADED/RECOGNIZING/DONE/FAILED；
     * keyword: 识别内容模糊搜索）
     */
    @GetMapping("/records")
    public Response<Map<String, Object>> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        List<AsrRecord> records = asrService.listRecords(source, status, keyword, page, size);
        long total = asrService.countRecords(source, status, keyword);
        return Response.success(Map.of(
                "records", records,
                "total", total,
                "page", page,
                "size", size
        ));
    }

    /**
     * 触发识别（重复调用覆盖上次结果），异步执行，前端轮询状态
     *
     * @param lang 识别语言 cn/en，默认cn
     */
    @PostMapping("/records/{id}/recognize")
    public Response<AsrRecord> recognize(@PathVariable String id,
                                         @RequestParam(defaultValue = "cn") String lang) {
        return Response.success(asrService.recognize(id, lang));
    }

    /**
     * 批量触发识别
     */
    @PostMapping("/records/batch-recognize")
    public Response<Void> recognizeBatch(@RequestBody List<String> recordIds,
                                         @RequestParam(defaultValue = "cn") String lang) {
        asrService.recognizeBatch(recordIds, lang);
        return Response.success();
    }

    /**
     * 删除记录及关联音频文件
     */
    @DeleteMapping("/records/{id}")
    public Response<Void> deleteRecord(@PathVariable String id) {
        asrService.deleteRecord(id);
        return Response.success();
    }

    /**
     * 批量删除记录及关联音频文件
     */
    @PostMapping("/records/batch-delete")
    public Response<Void> deleteRecords(@RequestBody List<String> recordIds) {
        asrService.deleteRecords(recordIds);
        return Response.success();
    }

    /**
     * 音频文件播放/下载（优先转码后WAV，其次原始文件）
     */
    @GetMapping("/records/{id}/audio")
    public ResponseEntity<FileSystemResource> audio(@PathVariable String id) {
        AsrRecord record = asrService.getRecord(id);
        String path = StrUtil.isNotBlank(record.getWavPath()) ? record.getWavPath() : record.getFilePath();
        if (StrUtil.isBlank(path)) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(xFileStorage.resolve(path).toFile());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/wav")
                .body(resource);
    }
}
