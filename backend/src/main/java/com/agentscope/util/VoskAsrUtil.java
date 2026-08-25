package com.agentscope.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Vosk 离线语音识别工具类
 * <p>
 * 支持中英文模型切换，默认中文。
 * 模型按语言懒加载，每种语言仅加载一次。
 * </p>
 */
public class VoskAsrUtil {

    /** 语言标识：中文（默认） */
    public static final String LANG_CN = "cn";
    /** 语言标识：英文 */
    public static final String LANG_EN = "en";
    /** 默认语言 */
    public static final String DEFAULT_LANG = LANG_CN;

    /** 各语言模型路径 */
    private static final Map<String, String> MODEL_PATHS = Map.of(
            LANG_CN, "/home/software/AI/vosk-model-small-cn-0.22",
            LANG_EN, "/home/software/AI/vosk-model-small-en-us-0.15"
    );

    // 每个语言独立的单例模型，避免重复加载
    private static final ConcurrentHashMap<String, Model> MODELS = new ConcurrentHashMap<>();

    private static final float SAMPLE_RATE = 16000.0f;

    static {
        // 修复 Java 11+ 环境下 JNI 返回中文乱码问题
        System.setProperty("jna.encoding", "UTF-8");
        System.setProperty("file.encoding", "UTF-8");
        // 设置 Vosk 内部日志级别（0为静默，-1为错误，1为信息）
        LibVosk.setLogLevel(LogLevel.DEBUG);
    }

    /**
     * 规范化语言参数：空/未知回落到默认中文
     */
    public static String normalizeLang(String lang) {
        if (StrUtil.isNotBlank(lang) && MODEL_PATHS.containsKey(lang.toLowerCase())) {
            return lang.toLowerCase();
        }
        return DEFAULT_LANG;
    }

    /**
     * 获取指定语言的共享模型实例（懒加载，每语言仅加载一次）
     *
     * @throws UncheckedIOException 模型路径不存在时抛出
     */
    public static Model getSharedModel(String lang) {
        String key = normalizeLang(lang);
        Model cached = MODELS.get(key);
        if (cached != null) {
            return cached;
        }
        synchronized (VoskAsrUtil.class) {
            cached = MODELS.get(key);
            if (cached != null) {
                return cached;
            }
            String path = MODEL_PATHS.get(key);
            File modelDir = new File(path);
            if (!modelDir.exists()) {
                throw new UncheckedIOException(new IOException("模型路径不存在: " + modelDir.getAbsolutePath()));
            }
            System.out.println(" 正在加载 Vosk[" + key + "] 模型...");
            Model m;
            try {
                m = new Model(modelDir.getAbsolutePath());
            } catch (IOException e) {
                throw new UncheckedIOException("加载Vosk模型失败: " + path, e);
            }
            System.out.println(" Vosk[" + key + "] 模型加载成功");
            MODELS.put(key, m);
            return m;
        }
    }

    /**
     * 创建指定语言的识别器（16kHz，每个会话独立持有，用完需 close），
     * 创建后自动喂入引导静音预热解码器，避免开头几个字被吞
     */
    public static Recognizer createRecognizer(String lang) {
        try {
            Recognizer recognizer = new Recognizer(getSharedModel(normalizeLang(lang)), SAMPLE_RATE);
            byte[] silence = leadSilence();
            recognizer.acceptWaveForm(silence, silence.length);
            return recognizer;
        } catch (IOException e) {
            throw new UncheckedIOException("创建Vosk识别器失败", e);
        }
    }

    /** 引导静音时长（毫秒）：解码器/iVector预热，防止起始语音丢失 */
    private static final int LEAD_SILENCE_MS = 300;

    /** 生成16kHz单声道16bit的零样本静音缓冲 */
    private static byte[] leadSilence() {
        return new byte[(int) (SAMPLE_RATE * 2 * LEAD_SILENCE_MS / 1000)];
    }

    /**
     * 识别 WAV 文件并提取纯文本（使用指定语言模型）
     *
     * @param filePath 16kHz 单声道 PCM WAV 文件路径
     * @param lang     语言标识（cn/en），空则默认中文
     * @return 识别文本（可能为空串）
     */
    public static String recognizeFileText(String filePath, String lang) throws Exception {
        Recognizer recognizer = createRecognizer(lang);
        try {
            byte[] audioData = Files.readAllBytes(new File(filePath).toPath());
            // 跳过WAV头（兼容非标准头），仅送PCM数据；引导静音已在createRecognizer中喂入
            int header = wavHeaderSize(audioData);
            if (audioData.length > header) {
                byte[] pcm = java.util.Arrays.copyOfRange(audioData, header, audioData.length);
                recognizer.acceptWaveForm(pcm, pcm.length);
            }
            String json = recognizer.getFinalResult();
            if (StrUtil.isBlank(json)) {
                return "";
            }
            String text = JSONUtil.parseObj(json).getStr("text", "");
            return text == null ? "" : text.trim();
        } finally {
            recognizer.close();
        }
    }

    /**
     * 解析WAV头实际大小（兼容非标准头），解析失败按标准44字节处理
     */
    private static int wavHeaderSize(byte[] data) {
        // RIFF....WAVE 后顺序查找 data 块
        for (int i = 12; i + 8 <= Math.min(data.length, 4096); ) {
            String id = new String(data, i, 4, StandardCharsets.US_ASCII);
            int size = (data[i + 4] & 0xFF) | (data[i + 5] & 0xFF) << 8
                    | (data[i + 6] & 0xFF) << 16 | (data[i + 7] & 0xFF) << 24;
            if ("data".equals(id)) {
                return i + 8;
            }
            i += 8 + size + (size & 1);
        }
        return 44;
    }

    /**
     * 实时麦克风识别（阻塞式，带回调），默认中文
     *
     * @param onResult  识别到完整句子时的回调
     * @param onPartial 实时部分识别结果的回调（可为 null）
     */
    public static void startRealtimeAsr(Consumer<String> onResult, Consumer<String> onPartial) throws Exception {
        startRealtimeAsr(DEFAULT_LANG, onResult, onPartial);
    }

    /**
     * 实时麦克风识别（阻塞式，带回调），可指定语言
     */
    public static void startRealtimeAsr(String lang, Consumer<String> onResult, Consumer<String> onPartial) throws Exception {
        Recognizer recognizer = createRecognizer(lang);

        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException(" 未找到支持的麦克风设备");
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        System.out.println("️ 麦克风已就绪！请开始说话（按 Ctrl+C 停止）");
        byte[] buffer = new byte[4096];
        int bytesRead;

        try {
            while ((bytesRead = line.read(buffer, 0, buffer.length)) >= 0) {
                if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                    String result = recognizer.getResult();
                    if (onResult != null && result != null) {
                        onResult.accept(result);
                    }
                } else {
                    if (onPartial != null) {
                        String partial = recognizer.getPartialResult();
                        onPartial.accept(partial);
                    }
                }
            }
        } finally {
            line.stop();
            line.close();
            recognizer.close();
        }
    }

    /**
     * 释放全部模型资源（在应用退出时调用）
     */
    public static void shutdown() {
        MODELS.values().forEach(Model::close);
        MODELS.clear();
        System.out.println(" Vosk 模型资源已释放");
    }

    public static void main(String[] args) {
        try {
            VoskAsrUtil.startRealtimeAsr(
                    result -> {
                        if (StrUtil.isNotBlank(result)) {
                            System.out.println("️ 完整结果: " + result);
                        }
                    },
                    partial -> {
                        String say = JSONUtil.parseObj(partial).getStr("partial");
                        if (StrUtil.isNotBlank(say)) {
                            System.out.println(" 正在说: " + say);
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            VoskAsrUtil.shutdown();
        }
    }
}
