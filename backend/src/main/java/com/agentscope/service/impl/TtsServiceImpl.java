package com.agentscope.service.impl;

import com.agentscope.service.TtsService;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Snowflake;
import com.agentscope.config.TtsConfig;
import com.agentscope.util.MeloTtsUtil;
import com.agentscope.util.SupertonicTtsUtil;
import com.agentscope.util.TtsTextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.OfflineTts;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TtsServiceImpl implements TtsService {

    private final Cache<String, TtsResult> memoryCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();

    @Value("${app.tts.edge-command:edge-tts}")
    private String edgeCommand;
    @Value("${app.tts.use-offline:false}")
    private Boolean useOffline;

    @Value("${app.tts.audio-dir:/data/agent-scope/tts-cache}")
    private String audioDir;
    @Resource
    TtsConfig ttsConfig;

    @Override
    public TtsResult speak(String text, String engine) {
        if (engine == null || engine.isBlank()) engine = "edge";
        String cacheKey = md5(text + ":" + engine);

        TtsResult cached = memoryCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }

        Path diskPath = Path.of(audioDir, engine, cacheKey + ".mp3");
        if (Files.exists(diskPath)) {
            try {
                byte[] audio = Files.readAllBytes(diskPath);
                TtsResult result = new TtsResult(audio, "audio/mpeg");
                memoryCache.put(cacheKey, result);
                return result;
            } catch (IOException e) {
                log.warn("读取磁盘缓存失败: {}", diskPath, e);
            }
        }

        TtsResult result = switch (engine) {
            case "edge" -> synthesizeEdge(text);
            default -> synthesizeEdge(text);
        };

        memoryCache.put(cacheKey, result);
        saveToDisk(diskPath, result.audio());
        return result;
    }

    @Override
    public void clearCache() {
        memoryCache.invalidateAll();
        try {
            Path dir = Path.of(audioDir);
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .filter(Files::isRegularFile)
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("清理磁盘缓存失败", e);
        }
    }

    @Override
    public Map<String, Object> getCacheStats() {
        return Map.of(
                "memorySize", memoryCache.estimatedSize(),
                "hitCount", memoryCache.stats().hitCount(),
                "missCount", memoryCache.stats().missCount()
        );
    }

    private TtsResult synthesizeEdge(String text) {
        if (useOffline) {
            //是否有中文
            boolean hasChinese = TtsTextUtil.hasChinese(text);
            if (!hasChinese) {
                try {
                    String ttsPath = ttsConfig.getPath() + File.separator + "supertonic" + File.separator + new Snowflake().nextIdStr() + ".wav";
                    SupertonicTtsUtil.synthesize(text, "en", ttsPath, false);
                    log.info("SupertonicTts={}", ttsPath);
                    return new TtsResult(FileUtil.readBytes(new File(ttsPath)), "audio/mpeg");
                } catch (Exception e) {
                    log.error("", e);
                }
            } else {
                if (text.trim().length() >= 5) {
                    try {
                        OfflineTts tts = MeloTtsUtil.getInstance();
                        File file = new File(ttsConfig.getPath() + File.separator + "melo");
                        if (file.mkdirs()) {
                            log.info("filePath={}", file);
                        }
                        String ttsPath = file.getPath() + File.separator + new Snowflake().nextIdStr() + ".wav";
                        GeneratedAudio audio = tts.generate(text, 0, ttsConfig.lengthScale);
                        audio.save(ttsPath);
                        log.info("MeloTts={}", ttsPath);
                        return new TtsResult(FileUtil.readBytes(new File(ttsPath)), "audio/mpeg");
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }
            }
        }

        try {
            Path tempFile = Files.createTempFile("tts_", ".mp3");
            ProcessBuilder pb = new ProcessBuilder(
                    edgeCommand,
                    "--voice", "zh-CN-XiaoxiaoNeural",
                    "--text", text,
                    "--write-media", tempFile.toString()
            );
            pb.redirectErrorStream(false);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String err = new String(process.getErrorStream().readAllBytes());
                throw new RuntimeException("edge-tts failed: " + err);
            }
            byte[] audio = Files.readAllBytes(tempFile);
            Files.deleteIfExists(tempFile);
            return new TtsResult(audio, "audio/mpeg");
        } catch (Exception e) {
            log.error("Edge TTS 合成失败", e);
            throw new RuntimeException("TTS synthesis failed", e);
        }
    }

    private void saveToDisk(Path path, byte[] data) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        } catch (IOException e) {
            log.warn("保存TTS缓存到磁盘失败: {}", path, e);
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
