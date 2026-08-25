package com.agentscope.util;

import cn.hutool.extra.spring.SpringUtil;
import com.agentscope.config.TtsConfig;
import com.k2fsa.sherpa.onnx.*;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * SupertonicTTS 3 Java 工具类 — 基于 sherpa-onnx 的高质量多语言语音合成。
 * <p>
 * 支持 31 种语言，多说话人，int8 量化，离线运行。
 * <p>
 * 使用前请先运行下载脚本:
 * <pre>
 * bash scripts/download-supertonic.sh
 * </pre>
 */
@Slf4j
public final class SupertonicTtsUtil {

    private static final String NATIVE_LIB_DIR = resolveNativeLibDir();
    private static final String MODEL_DIR = resolveModelDir();
    private static volatile OfflineTts sharedInstance;

    // ===================== 单例 =====================

    private SupertonicTtsUtil() {
    }

    public static OfflineTts getInstance() {
        OfflineTts tts = sharedInstance;
        if (tts != null) return tts;
        synchronized (SupertonicTtsUtil.class) {
            tts = sharedInstance;
            if (tts != null) return tts;
            sharedInstance = createTts();
            return sharedInstance;
        }
    }

    public static void releaseInstance() {
        synchronized (SupertonicTtsUtil.class) {
            if (sharedInstance != null) {
                sharedInstance.release();
                sharedInstance = null;
            }
        }
    }

    // ===================== API =====================

    /**
     * 合成到文件
     */
    public static int synthesize(String text, String lang, String wavPath) throws Exception {
        return synthesize(text, lang, wavPath, 0, 1.0f,true);
    }
    public static int synthesize(String text, String lang, String wavPath,boolean read) throws Exception {
        return synthesize(text, lang, wavPath, 0, 1.0f,read);
    }

    /**
     * 合成到文件（指定说话人、语速）
     */
    public static int synthesize(String text, String lang, String wavPath, int sid, float speed,boolean read) throws Exception {
        initNative();
        OfflineTts tts = getInstance();
        GeneratedAudio audio = generate(tts, text, lang, sid, speed);
        audio.save(wavPath);
        if(read){
            playAudio(audio);
        }
        log.info("[SupertonicTts] {} [{}] {} → {} ({} Hz)", lang, sid, text, wavPath, audio.getSampleRate());
        return audio.getSampleRate();
    }

    /**
     * 合成 + 朗读
     */
    public static void speak(String text, String lang) throws Exception {
        speak(text, lang, 0, 1.0f);
    }

    /**
     * 合成 + 朗读（指定说话人、语速）
     */
    public static void speak(String text, String lang, int sid, float speed) throws Exception {
        initNative();
        OfflineTts tts = getInstance();
        GeneratedAudio audio = generate(tts, text, lang, sid, speed);
        playAudio(audio);
    }

    /**
     * 合成到 byte[]
     */
    public static byte[] synthesizeToBytes(String text, String lang) {
        initNative();
        OfflineTts tts = getInstance();
        GeneratedAudio audio = generate(tts, text, lang, 0, 1.0f);
        return floatToBytes16(audio.getSamples());
    }

    /**
     * 合成到 float[]
     */
    public static float[] synthesizeToFloat(String text, String lang) {
        initNative();
        OfflineTts tts = getInstance();
        GeneratedAudio audio = generate(tts, text, lang, 0, 1.0f);
        return audio.getSamples();
    }

    /**
     * 批量合成
     */
    public static List<Integer> synthesizeBatch(String lang, List<String> texts, List<String> wavPaths) {
        if (texts.size() != wavPaths.size()) {
            throw new IllegalArgumentException("texts 与 wavPaths 大小不一致");
        }
        initNative();
        OfflineTts tts = getInstance();
        List<Integer> sampleRates = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            GeneratedAudio audio = generate(tts, texts.get(i), lang, 0, 1.0f);
            audio.save(wavPaths.get(i));
            sampleRates.add(audio.getSampleRate());
            log.info("[SupertonicTts] [{}/{}] {} → {} ({} Hz)", i + 1, texts.size(), texts.get(i), wavPaths.get(i), audio.getSampleRate());
        }
        return sampleRates;
    }

    /**
     * 批量朗读
     */
    public static void speakBatch(String lang, List<String> texts) throws Exception {
        initNative();
        OfflineTts tts = getInstance();
        for (int i = 0; i < texts.size(); i++) {
            log.info("[SupertonicTts] [{}/{}] 朗读: {}", i + 1, texts.size(), texts.get(i));
            GeneratedAudio audio = generate(tts, texts.get(i), lang, 0, 1.0f);
            playAudio(audio);
        }
    }

    // ===================== 内部 =====================

    private static GeneratedAudio generate(OfflineTts tts, String text, String lang, int sid, float speed) {
        GenerationConfig genConfig = new GenerationConfig();
        genConfig.setSid(sid);
        genConfig.setSpeed(speed);
        genConfig.setNumSteps(8);

        Map<String, String> extra = new HashMap<>();
        extra.put("lang", lang);
        genConfig.setExtra(extra);

        return tts.generateWithConfigAndCallback(text, genConfig, (OfflineTtsCallback) null);
    }

    private static OfflineTts createTts() {
        initNative();
        Path dir = Paths.get(MODEL_DIR);

        OfflineTtsSupertonicModelConfig supertonicConfig = OfflineTtsSupertonicModelConfig.builder()
                .setDurationPredictor(dir.resolve("duration_predictor.int8.onnx").toString())
                .setTextEncoder(dir.resolve("text_encoder.int8.onnx").toString())
                .setVectorEstimator(dir.resolve("vector_estimator.int8.onnx").toString())
                .setVocoder(dir.resolve("vocoder.int8.onnx").toString())
                .setTtsJson(dir.resolve("tts.json").toString())
                .setUnicodeIndexer(dir.resolve("unicode_indexer.bin").toString())
                .setVoiceStyle(dir.resolve("voice.bin").toString())
                .build();

        OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder()
                .setSupertonic(supertonicConfig)
                .setNumThreads(2)
                .setDebug(false)
                .build();

        OfflineTtsConfig config = OfflineTtsConfig.builder()
                .setModel(modelConfig)
                .build();

        log.info("[SupertonicTts] 创建实例, model={}", MODEL_DIR);
        return new OfflineTts(config);
    }

    // ===================== native =====================

    private static void initNative() {
        System.setProperty("sherpa.onnx.lib.dir", NATIVE_LIB_DIR);
    }

    // ===================== 工具 =====================

    public static byte[] floatToBytes16(float[] samples) {
        ByteBuffer buf = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (float s : samples) {
            buf.putShort((short) (Math.max(-1f, Math.min(1f, s)) * 32767));
        }
        return buf.array();
    }

    public static void playAudio(GeneratedAudio audio) throws Exception {
        byte[] pcm = floatToBytes16(audio.getSamples());
        AudioFormat fmt = new AudioFormat(audio.getSampleRate(), 16, 1, true, false);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(
                new javax.sound.sampled.DataLine.Info(SourceDataLine.class, fmt));
        line.open(fmt);
        line.start();
        line.write(pcm, 0, pcm.length);
        line.drain();
        line.close();
    }

    // ===================== 路径 =====================

    private static String resolveNativeLibDir() {
        String userDir = System.getProperty("user.dir", "");
        String[] candidates = {
                userDir + "/sherpa-onnx"
        };
        for (String c : candidates) {
            if (Files.exists(Paths.get(c, "libsherpa-onnx-jni.so"))) return c;
        }
        return getOnnxPath();
    }

    public static String getOnnxPath() {
        String path;
        TtsConfig bean = SpringUtil.getBean(TtsConfig.class);
        path = Objects.requireNonNullElseGet(bean, TtsConfig::new).getPath();
        return path;
    }

    private static String resolveModelDir() {
        String base = NATIVE_LIB_DIR + "/sherpa-onnx-supertonic-3-tts-int8-2026-05-11";
        if (Files.exists(Paths.get(base, "tts.json"))) return base;
        return "./sherpa-onnx/sherpa-onnx-supertonic-3-tts-int8-2026-05-11";
    }

    // ===================== main =====================

    public static void main(String[] args) throws Exception {
        log.info("[SupertonicTts] ========================================");
        log.info("[SupertonicTts] SupertonicTTS 3 测试");
        log.info("[SupertonicTts] ========================================");

        initNative();

        // 英文
        synthesize("1", "en", "/tmp/supertonic_en.wav");
        synthesize("2", "en", "/tmp/supertonic_en.wav");
        synthesize("3", "en", "/tmp/supertonic_en.wav");
        synthesize("a", "en", "/tmp/supertonic_en.wav");
        synthesize("b", "en", "/tmp/supertonic_en.wav");
        synthesize("c", "en", "/tmp/supertonic_en.wav");
        synthesize("apple", "en", "/tmp/supertonic_en.wav");
        synthesize("hi", "en", "/tmp/supertonic_en.wav");
        synthesize("red", "en", "/tmp/supertonic_en.wav");


        synthesize("Today as always, men fall into two groups: slaves and free men.", "en", "/tmp/supertonic_en.wav");

        // 中文
        //synthesize("你好世界！这是 SupertonicTTS 3 语音合成。", "zh", "/tmp/supertonic_zh.wav");

        // 日文
        //synthesize("こんにちは世界！", "ja", "/tmp/supertonic_ja.wav");

        // 韩文
        synthesize("안녕하세요 세계!", "ko", "/tmp/supertonic_ko.wav");

        // 法文
        synthesize("Bonjour le monde!", "fr", "/tmp/supertonic_fr.wav");

        log.info("[SupertonicTts] 完成!");
    }
}
