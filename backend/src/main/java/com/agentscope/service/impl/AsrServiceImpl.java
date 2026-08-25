package com.agentscope.service.impl;

import cn.hutool.core.util.StrUtil;
import com.agentscope.common.ResultCode;
import com.agentscope.common.exception.BusinessException;
import com.agentscope.config.AsrConfig;
import com.agentscope.events.BizEventType;
import com.agentscope.events.EventPublisher;
import com.agentscope.model.entity.AsrRecord;
import com.agentscope.repository.AsrRecordRepository;
import com.agentscope.service.AsrService;
import com.agentscope.service.XFileStorageService;
import com.agentscope.service.XFileStorageService;
import com.agentscope.util.VoskAsrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 语音识别服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsrServiceImpl implements AsrService {

    private final AsrRecordRepository asrRecordRepository;
    private final AsrConfig asrConfig;
    private final MongoTemplate mongoTemplate;
    private final XFileStorageService xFileStorage;
    private final EventPublisher eventPublisher;

    @Override
    public List<AsrRecord> uploadAudios(List<MultipartFile> files) {
        List<AsrRecord> records = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalName = file.getOriginalFilename();
            if (StrUtil.isBlank(originalName)) {
                originalName = "audio_" + System.currentTimeMillis();
            }

            String ext = getExtension(originalName);
            if (!isSupportedAudio(ext)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的音频格式: " + originalName);
            }

            String key = storeFile(file, ext);

            records.add(asrRecordRepository.save(AsrRecord.builder()
                    .name(originalName)
                    .filePath(key)
                    .fileSize(file.getSize())
                    .status(AsrRecord.STATUS_UPLOADED)
                    .source(AsrRecord.SOURCE_FILE)
                    .build()));
        }
        return records;
    }

    @Override
    public List<AsrRecord> listRecords(String source, String status, String keyword, int page, int size) {
        Query query = buildRecordQuery(source, status, keyword);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        query.with(pageable);
        return mongoTemplate.find(query, AsrRecord.class);
    }

    @Override
    public long countRecords(String source, String status, String keyword) {
        return mongoTemplate.count(buildRecordQuery(source, status, keyword), AsrRecord.class);
    }

    private Query buildRecordQuery(String source, String status, String keyword) {
        Query query = new Query();
        if (StrUtil.isNotBlank(source)) {
            query.addCriteria(Criteria.where("source").is(source));
        }
        if (StrUtil.isNotBlank(status)) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (StrUtil.isNotBlank(keyword)) {
            // 转义正则特殊字符，按识别内容不区分大小写模糊匹配
            String escaped = java.util.regex.Pattern.quote(keyword.trim());
            query.addCriteria(Criteria.where("text").regex(escaped, "i"));
        }
        return query;
    }

    @Override
    public AsrRecord recognize(String recordId, String lang) {
        AsrRecord record = asrRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "识别记录不存在"));
        if (record.getSource().equals(AsrRecord.SOURCE_FILE)) {
            // 记录本次使用的语言，重复识别可切换；通过事件总线异步执行
            record.setLanguage(VoskAsrUtil.normalizeLang(lang));
            markRecognizing(record);
            String finalLang = record.getLanguage();
            eventPublisher.publish(BizEventType.ASR_RECOGNIZE, recordId,
                    Map.of("lang", finalLang));
        }
        return record;
    }

    @Override
    public void recognizeNow(String recordId, String lang) {
        doRecognize(recordId, lang);
    }

    @Override
    public void recognizeBatch(List<String> recordIds, String lang) {
        for (String id : recordIds) {
            try {
                recognize(id, lang);
            } catch (Exception e) {
                log.error("批量识别提交失败: recordId={}", id, e);
            }
        }
    }

    /**
     * 执行识别：确保16k单声道WAV（必要时ffmpeg转码）→ Vosk识别 → 保存文本。
     * 多次识别直接覆盖text字段。
     */
    private void doRecognize(String recordId, String lang) {
        try {
            AsrRecord record = asrRecordRepository.findById(recordId).orElse(null);
            if (record == null) return;

            String wavPath = ensureWav16kMono(record);
            String text = VoskAsrUtil.recognizeFileText(wavPath, lang);

            record.setText(text);
            record.setWavPath(wavPath);
            record.setStatus(AsrRecord.STATUS_DONE);
            record.setErrorMessage(null);
            asrRecordRepository.save(record);
            log.info("语音识别完成: id={}, name={}, textLength={}", recordId, record.getName(), text.length());
        } catch (Exception e) {
            log.error("语音识别失败: recordId={}", recordId, e);
            asrRecordRepository.findById(recordId).ifPresent(r -> {
                r.setStatus(AsrRecord.STATUS_FAILED);
                r.setErrorMessage(StrUtil.maxLength(String.valueOf(e.getMessage()), 500));
                asrRecordRepository.save(r);
            });
        }
    }

    private void markRecognizing(AsrRecord record) {
        record.setStatus(AsrRecord.STATUS_RECOGNIZING);
        asrRecordRepository.save(record);
    }

    /**
     * 保证存在16kHz单声道PCM WAV；若已转码直接复用，否则用ffmpeg转码（覆盖旧的转码文件）
     */
    private synchronized String ensureWav16kMono(AsrRecord record) throws IOException, InterruptedException {
        // filePath可能为统一存储key或历史绝对路径，统一经门面解析
        File source = xFileStorage.resolve(record.getFilePath()).toFile();
        if (!source.exists()) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "音频文件不存在");
        }

        // 已有转码文件则复用（重复识别无需再次转码）
        if (StrUtil.isNotBlank(record.getWavPath())) {
            File wav = new File(record.getWavPath());
            if (wav.exists()) {
                return wav.getAbsolutePath();
            }
        }

        Files.createDirectories(Paths.get(asrConfig.getAudioDir()));
        String baseName = source.getName().replaceAll("\\.[^.]+$", "");
        Path target = Paths.get(asrConfig.getAudioDir(),
                baseName + "_" + UUID.randomUUID().toString().substring(0, 8) + ".wav");

        ProcessBuilder pb = new ProcessBuilder(
                asrConfig.getFfmpegPath(), "-y", "-hide_banner", "-loglevel", "error",
                "-i", source.getAbsolutePath(),
                "-ar", "16000", "-ac", "1", "-sample_fmt", "s16",
                target.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int code = process.waitFor();
        if (code != 0 || !Files.exists(target)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "音频转码失败(ffmpeg exit=" + code + ")");
        }
        return target.toString();
    }

    @Override
    public void deleteRecord(String recordId) {
        AsrRecord record = asrRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "识别记录不存在"));

        // 业务资产走统一存储删除（兼容旧绝对路径）；转码WAV为临时产物直接清理
        xFileStorage.delete(record.getFilePath());
        deleteQuietly(record.getWavPath());
        asrRecordRepository.deleteById(recordId);
    }

    @Override
    public void deleteRecords(List<String> recordIds) {
        for (String id : recordIds) {
            try {
                deleteRecord(id);
            } catch (Exception e) {
                log.error("批量删除识别记录失败: recordId={}", id, e);
            }
        }
    }

    @Override
    public AsrRecord getRecord(String recordId) {
        return asrRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "识别记录不存在"));
    }

    @Override
    public AsrRecord saveRealtimeResult(String recordId, String name, String text, Long durationSeconds, String lang) {
        AsrRecord record;
        if (StrUtil.isNotBlank(recordId)) {
            record = asrRecordRepository.findById(recordId)
                    .orElseGet(() -> newRecord(name));
        } else {
            record = newRecord(name);
        }
        record.setName(StrUtil.isNotBlank(name) ? name : record.getName());
        record.setText(text);
        record.setDuration(durationSeconds != null ? durationSeconds.doubleValue() : null);
        record.setStatus(AsrRecord.STATUS_DONE);
        record.setSource(AsrRecord.SOURCE_REALTIME);
        record.setLanguage(VoskAsrUtil.normalizeLang(lang));
        return asrRecordRepository.save(record);
    }

    @Override
    public void attachAudio(String recordId, byte[] wavBytes) {
        if (wavBytes == null || wavBytes.length == 0) return;
        try {
            // 通过x-file-storage统一存储（module=asr-rt，按recordId固定文件名覆盖）
            String key = xFileStorage.storeBytes(wavBytes, "asr-rt", recordId, recordId + ".wav");
            asrRecordRepository.findById(recordId).ifPresent(r -> {
                r.setFilePath(key);
                r.setFileSize((long) wavBytes.length);
                asrRecordRepository.save(r);
            });
        } catch (Exception e) {
            log.error("实时录音文件保存失败: recordId={}", recordId, e);
        }
    }

    private AsrRecord newRecord(String name) {
        return AsrRecord.builder()
                .name(StrUtil.isNotBlank(name) ? name : "实时录音_" + System.currentTimeMillis())
                .source(AsrRecord.SOURCE_REALTIME)
                .build();
    }

    private void deleteQuietly(String path) {
        if (StrUtil.isBlank(path)) return;
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.warn("删除文件失败: {}", path, e);
        }
    }

    private String storeFile(MultipartFile file, String ext) {
        try {
            // 通过x-file-storage统一存储（module=asr）
            return xFileStorage.store(file, "asr", null);
        } catch (IOException e) {
            log.error("音频文件存储失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "音频文件存储失败");
        }
    }

    private boolean isSupportedAudio(String ext) {
        return switch (ext) {
            case "wav", "mp3", "m4a", "aac", "ogg", "flac", "wma", "amr", "opus", "webm" -> true;
            default -> false;
        };
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }
}
