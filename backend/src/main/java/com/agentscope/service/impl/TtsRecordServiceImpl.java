package com.agentscope.service.impl;

import cn.hutool.core.util.StrUtil;
import com.agentscope.common.ResultCode;
import com.agentscope.common.exception.BusinessException;
import com.agentscope.events.BizEventType;
import com.agentscope.events.EventPublisher;
import com.agentscope.model.entity.TtsRecord;
import com.agentscope.model.entity.TtsSegment;
import com.agentscope.repository.TtsRecordRepository;
import com.agentscope.service.XFileStorageService;
import com.agentscope.service.TtsRecordService;
import com.agentscope.service.XFileStorageService;
import com.agentscope.service.TtsService;
import com.agentscope.util.TtsTextSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 语音合成记录服务实现。
 * <p>
 * 合成流程（异步）：按模式切分 → 逐段调用TTS合成 → ffprobe探测各段时长
 * → ffmpeg拼接为完整音频（统一重编码，兼容mp3/wav混输）→ 清理分段临时文件。
 * 分段仅保留文本+偏移+时长，用于前端播放高亮定位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsRecordServiceImpl implements TtsRecordService {

    private final TtsRecordRepository ttsRecordRepository;
    private final TtsService ttsService;
    private final MongoTemplate mongoTemplate;
    private final XFileStorageService xFileStorage;
    private final EventPublisher eventPublisher;

    @Value("${app.tts-record.audio-dir:/data/agent-scope/tts-records}")
    private String tempDir;

    /** 单篇文字上限 */
    private static final int MAX_TEXT_LENGTH = 50000;

    @Override
    public TtsRecord create(String title, String text, String mode) {
        if (StrUtil.isBlank(text)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "合成内容不能为空");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "单次最多合成" + MAX_TEXT_LENGTH + "字");
        }
        List<TtsSegment> segments = TtsTextSplitter.split(text, mode);
        if (segments.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未解析到可合成的有效内容");
        }
        String finalTitle = StrUtil.isNotBlank(title) ? title.trim()
                : StrUtil.maxLength(segments.get(0).getText().replaceAll("\\s+", " "), 40);

        TtsRecord record = TtsRecord.builder()
                .title(finalTitle)
                .text(text)
                .mode(StrUtil.blankToDefault(mode, TtsRecord.MODE_LINE))
                .status(TtsRecord.STATUS_SYNTHESIZING)
                .segments(segments)
                .charCount(text.length())
                .build();
        record = ttsRecordRepository.save(record);

        String recordId = record.getId();
        try {
            // 通过事件总线异步执行
            eventPublisher.publish(BizEventType.TTS_SYNTHESIZE, recordId, null);
        } catch (Exception e) {
            log.error("提交合成任务失败: recordId={}", recordId, e);
            markFailed(recordId, "任务提交失败: " + e.getMessage());
        }
        return record;
    }

    @Override
    public void synthesizeNow(String recordId) {
        doSynthesize(recordId);
    }

    /**
     * 执行合成：逐段TTS → 探测时长 → 拼接 → 收尾。
     * 任一段失败则整体标记FAILED并清理临时目录。
     */
    private void doSynthesize(String recordId) {
        Path tempDirForRecord = Path.of(tempDir, recordId);
        Path dir = tempDirForRecord;
        try {
            Files.createDirectories(dir);
            TtsRecord record = ttsRecordRepository.findById(recordId).orElse(null);
            if (record == null) return;

            List<TtsSegment> segments = record.getSegments();
            List<Path> chunkFiles = new ArrayList<>(segments.size());

            for (int i = 0; i < segments.size(); i++) {
                TtsSegment seg = segments.get(i);
                byte[] audio = ttsService.speak(seg.getText(), "edge").audio();
                Path chunk = dir.resolve("chunk_" + i + detectExt(audio));
                Files.write(chunk, audio, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                chunkFiles.add(chunk);
                double durationSec = probeDuration(chunk);
                seg.setDuration(durationSec > 0 ? durationSec : null);
                log.debug("分段合成完成: recordId={}, idx={}, 时长={}s", recordId, i, durationSec);
            }

            // 拼接为完整音频（重编码统一格式，避免mp3/wav混杂）
            Path full = dir.resolve("full.mp3");
            concatAudio(chunkFiles, full);
            double totalDuration = probeDuration(full);
            if (totalDuration <= 0) {
                // 拼接文件探测失败时退化为各段之和
                totalDuration = segments.stream()
                        .map(TtsSegment::getDuration)
                        .filter(d -> d != null && d > 0)
                        .mapToDouble(Double::doubleValue).sum();
            }

            // 完整音频通过x-file-storage统一存储（module=tts）
            byte[] audioBytes = Files.readAllBytes(full);

            // 清理分段临时文件与本地拼接产物，仅保留托管音频
            for (Path chunk : chunkFiles) {
                Files.deleteIfExists(chunk);
            }
            Files.deleteIfExists(full);

            String audioKey = xFileStorage.storeBytes(audioBytes, "tts", recordId, "full.mp3");

            record.setStatus(TtsRecord.STATUS_DONE);
            record.setSegments(segments);
            record.setAudioPath(audioKey);
            record.setDuration(totalDuration);
            record.setFileSize((long) audioBytes.length);
            record.setErrorMessage(null);
            ttsRecordRepository.save(record);
            log.info("合成完成: recordId={}, 段数={}, 总时长={}s", recordId, segments.size(), totalDuration);
        } catch (Exception e) {
            log.error("合成失败: recordId={}", recordId, e);
            markFailed(recordId, e.getMessage());
            cleanupDir(dir);
        }
    }

    /** 根据文件头判断扩展名：RIFF=wav，其余按mp3处理 */
    private String detectExt(byte[] audio) {
        if (audio != null && audio.length > 12
                && audio[0] == 'R' && audio[1] == 'I' && audio[2] == 'F' && audio[3] == 'F') {
            return ".wav";
        }
        return ".mp3";
    }

    /** ffmpeg concat重编码拼接，输出24kHz单声道mp3 */
    private void concatAudio(List<Path> chunks, Path output) throws IOException, InterruptedException {
        Path listFile = output.getParent().resolve("concat_list.txt");
        StringBuilder sb = new StringBuilder();
        for (Path chunk : chunks) {
            sb.append("file '").append(chunk.toAbsolutePath()).append("'\n");
        }
        Files.writeString(listFile, sb.toString(), StandardCharsets.UTF_8);

        Process p = new ProcessBuilder(
                "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
                "-f", "concat", "-safe", "0", "-i", listFile.toString(),
                "-ar", "24000", "-ac", "1", "-b:a", "48k",
                output.toString())
                .redirectErrorStream(true)
                .start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int code = p.waitFor();
        Files.deleteIfExists(listFile);
        if (code != 0 || !Files.exists(output)) {
            throw new IOException("音频拼接失败(ffmpeg exit=" + code + "): " + out);
        }
    }

    /** ffprobe探测音频时长（秒），失败返回-1 */
    private double probeDuration(Path file) {
        try {
            Process p = new ProcessBuilder(
                    "ffprobe", "-v", "error", "-show_entries", "format=duration",
                    "-of", "csv=p=0", file.toString())
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            p.waitFor();
            return Double.parseDouble(out.split("\\s+")[0]);
        } catch (Exception e) {
            log.warn("时长探测失败: {}", file, e);
            return -1;
        }
    }

    private void markFailed(String recordId, String message) {
        try {
            ttsRecordRepository.findById(recordId).ifPresent(r -> {
                r.setStatus(TtsRecord.STATUS_FAILED);
                r.setErrorMessage(StrUtil.maxLength(message, 500));
                ttsRecordRepository.save(r);
            });
        } catch (Exception ex) {
            log.error("标记失败状态异常: recordId={}", recordId, ex);
        }
    }

    private void cleanupDir(Path dir) {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            log.warn("清理目录失败: {}", dir, e);
        }
    }

    @Override
    public List<TtsRecord> listRecords(String status, String keyword, int page, int size) {
        Query query = buildRecordQuery(status, keyword);
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 200));
        query.with(pageable);
        return mongoTemplate.find(query, TtsRecord.class);
    }

    @Override
    public long countRecords(String status, String keyword) {
        return mongoTemplate.count(buildRecordQuery(status, keyword), TtsRecord.class);
    }

    private Query buildRecordQuery(String status, String keyword) {
        Query query = new Query();
        if (StrUtil.isNotBlank(status)) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (StrUtil.isNotBlank(keyword)) {
            // 转义正则特殊字符，标题或内容模糊匹配
            String escaped = java.util.regex.Pattern.quote(keyword.trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(escaped, "i"),
                    Criteria.where("text").regex(escaped, "i")));
        }
        return query;
    }

    @Override
    public TtsRecord getRecord(String recordId) {
        return ttsRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "合成记录不存在"));
    }

    @Override
    public void deleteRecord(String recordId) {
        TtsRecord record = getRecord(recordId);
        cleanupDir(Path.of(tempDir, record.getId()));
        xFileStorage.delete(record.getAudioPath());
        ttsRecordRepository.deleteById(record.getId());
    }

    @Override
    public void deleteRecords(List<String> recordIds) {
        for (String id : recordIds) {
            try {
                deleteRecord(id);
            } catch (Exception e) {
                log.error("批量删除合成记录失败: recordId={}", id, e);
            }
        }
    }

}
