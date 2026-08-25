package com.agentscope.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Snowflake;
import com.agentscope.config.TtsConfig;
import com.k2fsa.sherpa.onnx.*;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * MeloTTS Java 工具类 — 基于 sherpa-onnx 的本地离线语音合成。
 * <p>
 * 使用 sherpa-onnx 的 MeloTTS zh_en ONNX 模型，支持中英文混合合成。
 * 无需 Python 服务，纯 Java 实现，CPU 实时推理。
 * <p>
 * IDEA 直接运行 main 即可，无需任何 JVM 参数。
 * <p>
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * <b>参数详解</b>
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────────┐
 * │  VITS 模型参数 (OfflineTtsVitsModelConfig)                                     │
 * ├────────────────────┬──────────────────┬─────────────────────────────────────────┤
 * │ 参数名              │ 默认值           │ 说明                                    │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ noiseScale         │ 0.667            │ 噪声比例，控制合成语音的随机性/变化性。     │
 * │                    │                  │   值越大 → 语音越有变化、自然，           │
 * │                    │                  │   值越小 → 语音越稳定、单调。             │
 * │                    │                  │   范围: 0.0 ~ 1.0                       │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ noiseScaleW        │ 0.8              │ 长噪声比例，控制时长预测的噪声。          │
 * │                    │                  │   值越大 → 语速变化越大、节奏更自然，     │
 * │                    │                  │   值越小 → 语速越均匀、越机械。           │
 * │                    │                  │   范围: 0.0 ~ 1.0                       │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ lengthScale        │ 1.0              │ 语速缩放因子。                           │
 * │                    │                  │   < 1.0 → 加速（快语速），               │
 * │                    │                  │   = 1.0 → 原始速度，                     │
 * │                    │                  │   > 1.0 → 减速（慢语速）。               │
 * │                    │                  │   范围: 0.1 ~ 3.0                       │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ tokens             │ tokens.txt       │ 词表文件路径，定义模型支持的所有 token。  │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ lexicon            │ lexicon.txt      │ 词典文件路径，提供字 → 音素的映射。      │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ dictDir            │ dict/            │ 字典目录，包含 FST 格式的分词规则。       │
 * └────────────────────┴──────────────────┴─────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────┐
 * │  模型通用参数 (OfflineTtsModelConfig)                                           │
 * ├────────────────────┬──────────────────┬─────────────────────────────────────────┤
 * │ numThreads         │ 2                │ ONNX Runtime 推理线程数。               │
 * │                    │                  │   CPU 核心多时可调大，提升合成速度。      │
 * │                    │                  │   建议: 1 ~ 8                           │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ debug              │ false            │ 调试模式，开启后输出详细日志。            │
 * │                    │                  │   排查问题时建议开启。                    │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ provider           │ "cpu"            │ 推理后端: "cpu" (默认) 或 "cuda"。      │
 * │                    │                  │   CUDA 需要 GPU + CUDA toolkit。        │
 * └────────────────────┴──────────────────┴─────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────┐
 * │  全局配置 (OfflineTtsConfig)                                                    │
 * ├────────────────────┬──────────────────┬─────────────────────────────────────────┤
 * │ ruleFsts           │ date/number/     │ FST 规则文件路径（逗号分隔），用于        │
 * │                    │ phone.fst        │   日期/数字/电话号码等文本的标准化处理。  │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ ruleFars           │ ""               │ FAR 规则文件路径（逗号分隔），更复杂的    │
 * │                    │                  │   文本归一化规则。                        │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ maxNumSentences    │ 1                │ 单次合成最大句子数。                      │
 * │                    │                  │   增大可一次合成多句，但消耗更多内存。    │
 * ├────────────────────┼──────────────────┼─────────────────────────────────────────┤
 * │ silenceScale       │ 0.2              │ 静音比例缩放，控制句子间停顿时长。        │
 * │                    │                  │   值越大 → 停顿越长，                     │
 * │                    │                  │   值越小 → 停顿越短。                     │
 * └────────────────────┴──────────────────┴─────────────────────────────────────────┘
 * </pre>
 * ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 * <b>使用示例</b>
 * <pre>
 * // 1. 默认参数合成 + 朗读
 * MeloTtsUtil.main(new String[]{"你好世界"});
 *
 * // 2. 慢语速 + 保存文件
 * MeloTtsUtil.TtsConfig cfg = MeloTtsUtil.TtsConfig.builder()
 *     .lengthScale(1.3f)
 *     .build();
 * MeloTtsUtil.synthesize(cfg, "今天天气真好", "output.wav");
 *
 * // 3. 快语速 + 高噪声（更有变化）
 * MeloTtsUtil.TtsConfig cfg2 = MeloTtsUtil.TtsConfig.builder()
 *     .lengthScale(0.8f)
 *     .noiseScale(0.9f)
 *     .numThreads(4)
 *     .build();
 * MeloTtsUtil.speak(cfg2, "Hello world, 你好！");
 * </pre>
 */
@Slf4j
public final class MeloTtsUtil {

    private static final String NATIVE_LIB_DIR = resolveNativeLibDir();
    private static final String MODEL_DIR = resolveModelDir();

    private MeloTtsUtil() {}

    // ===================== 15 场景自动匹配 =====================

    /**
     * 短句阈值：字数 ≤ 此值为短句
     */
    private static final int SHORT_THRESHOLD = 15;


    // ===================== 单例 =====================

    private static volatile OfflineTts sharedInstance;
    private static volatile TtsConfig sharedConfig;

    /**
     * 获取默认配置的单例（懒加载，double-checked locking）
     */
    public static OfflineTts getInstance() {
        return getInstance(new TtsConfig());
    }

    /**
     * 获取指定配置的单例。配置变化时自动重建。
     */
    public static OfflineTts getInstance(TtsConfig cfg) {
        OfflineTts tts = sharedInstance;
        if (tts != null && sharedConfig != null && sharedConfig.equals(cfg)) {
            return tts;
        }
        synchronized (MeloTtsUtil.class) {
            tts = sharedInstance;
            if (tts != null && sharedConfig != null && sharedConfig.equals(cfg)) {
                return tts;
            }
            if (tts != null) {
                tts.release();
            }
            initNative();
            sharedInstance = createTts(cfg);
            sharedConfig = cfg;
            return sharedInstance;
        }
    }

    /**
     * 释放单例，下次调用时重新创建
     */
    public static void releaseInstance() {
        synchronized (MeloTtsUtil.class) {
            if (sharedInstance != null) {
                sharedInstance.release();
                sharedInstance = null;
                sharedConfig = null;
            }
        }
    }

    // ===================== 文本预处理 =====================


    // ===================== 单条 API =====================

    /**
     * 多线程安全合成：自动根据文本长度选择配置（短文本→WORD，长句→SENTENCE）。
     * generate() 在 C++ 层是 const 方法，天然线程安全。
     *
     * @param text    要合成的文本
     * @param wavPath 输出 WAV 文件路径
     */
    public static void synthesizeThreadSafe(String text, String wavPath) {
        initNative();
        TtsConfig cfg = new TtsConfig() ;
        OfflineTts tts = getInstance(cfg);
        GeneratedAudio audio = tts.generate(text, 0, cfg.lengthScale);
        audio.save(wavPath);
        log.info("[MeloTts] [thread-{}] {} → {} ({} Hz)", Thread.currentThread().getName(), text, wavPath, audio.getSampleRate());
    }

    /**
     * 合成 wav 字节数组（自动选配置）
     *
     * @param text
     * @param wavPath
     * @return
     */
    public static byte[] getWavBytes(String text, String wavPath) {
        synthesizeThreadSafe(text, wavPath);
        byte[] bytes = FileUtil.readBytes(wavPath);
        FileUtil.del(wavPath);
        return bytes;
    }



    private static String resolveNativeLibDir() {
        String userDir = System.getProperty("user.dir", "");
        String[] candidates = {
                userDir + "/sherpa-onnx",
                userDir + "/lib/sherpa-onnx"
        };
        for (String c : candidates) {
            if (Files.exists(Paths.get(c, "libsherpa-onnx-jni.so"))) return c;
        }
        return SupertonicTtsUtil.getOnnxPath();
    }

    /**
     * 根据配置创建 OfflineTts 实例（每次新建，不复用）
     */
    private static OfflineTts createTts(TtsConfig cfg) {
        initNative();
        Path dir = Paths.get(MODEL_DIR);
        String ruleFsts = String.join(",",
                dir.resolve("date.fst").toString(),
                dir.resolve("number.fst").toString(),
                dir.resolve("phone.fst").toString());

        OfflineTtsVitsModelConfig vits = OfflineTtsVitsModelConfig.builder()
                .setModel(dir.resolve("model.onnx").toString())
                .setTokens(dir.resolve("tokens.txt").toString())
                .setLexicon(dir.resolve("lexicon.txt").toString())
                .setDictDir(dir.resolve("dict").toString())
                .setLengthScale(cfg.lengthScale)
                .setNoiseScale(cfg.noiseScale)
                .setNoiseScaleW(cfg.noiseScaleW)
                .build();

        OfflineTtsModelConfig model = OfflineTtsModelConfig.builder()
                .setVits(vits)
                .setNumThreads(cfg.numThreads)
                .setDebug(cfg.debug)
                .build();

        OfflineTtsConfig config = OfflineTtsConfig.builder()
                .setModel(model)
                .setRuleFsts(ruleFsts)
                .setMaxNumSentences(cfg.maxNumSentences)
                .setSilenceScale(cfg.silenceScale)
                .build();

        return new OfflineTts(config);
    }

    // ===================== native 库加载 =====================

    private static void initNative() {
        System.setProperty("sherpa.onnx.lib.dir", NATIVE_LIB_DIR);
    }

    // ===================== 工具方法 =====================

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

    // ===================== 路径解析 =====================



    private static String resolveModelDir() {
        String base = NATIVE_LIB_DIR + "/vits-melo-tts-zh_en";
        if (Files.exists(Paths.get(base, "model.onnx"))) return base;
        return "";
    }

    // ===================== main 入口 =====================

    public static void main(String[] args) throws Exception {
        List<String> texts = new ArrayList<>();


        texts.add("你好阿");
        texts.add("谢谢你");
        texts.add("再见A");
        texts.add("苹果B");
        texts.add("香蕉C");
        texts.add("西瓜1");
        texts.add("小狗2");
        texts.add("小猫");
        texts.add("蝴蝶");
        texts.add("蜻蜓");
        texts.add("学校");
        texts.add("老师");

        // 场景5: 纯中文短句
        texts.add("你好，今天天气真好！");
        texts.add("我想吃一碗热腾腾的面条。");
        texts.add("小明背着书包去上学。");
        texts.add("春天来了，花儿开了。");
        texts.add("妈妈在厨房里做饭。");

        // 场景6: 纯英文短句
        texts.add("The weather is nice today.");
        texts.add("I have a red apple.");
        texts.add("Can you help me, please?");
        texts.add("The cat is sleeping.");
        texts.add("I like to read books.");

        // 场景7: 中英混合短句
        texts.add("我有一个iPhone手机。");
        texts.add("这个Excel表格很好用。");
        texts.add("请打开Word文档。");

        // 场景8: 含数字的短句
        texts.add("今天是八月二十一日。");
        texts.add("我有三个苹果。");
        texts.add("这本书有二百页。");

        // 场景9: 中英数字混合短句
        texts.add("今天是2026年8月21日。");
        texts.add("我有3个iPhone手机。");
        texts.add("这台电脑16GB内存。");

        // 场景10~14: 长句
        texts.add("春天来了，花儿开了，鸟儿在树上唱歌，小朋友们在草地上快乐地玩耍。");
        texts.add("The quick brown fox jumps over the lazy dog near the river bank.");
        texts.add("Today we are going to learn about the solar system and all the planets.");
        texts.add("小明今年10岁了，他在北京大学学习computer science专业。");
        texts.add("这本书一共有356页，售价是99元人民币，大约等于15美元。");

        // 场景15: 长段落
        texts.add("从前有一个美丽的小村庄，村子里住着一个善良的小女孩。"
                + "她每天都去山上采花，把花送给村子里的老人们。"
                + "大家都很喜欢她，叫她花仙子。");




        log.info("[MeloTts] ========================================");
        log.info("[MeloTts] MeloTTS 15 场景自动匹配测试");
        log.info("[MeloTts] ========================================");
        log.info("[MeloTts] 文本数: {}", texts.size());
        log.info("[MeloTts] 模型: {}", MODEL_DIR);
        log.info("[MeloTts] ----------------------------------------");

//        initNative();
//
//        int wordCount = 0, sentCount = 0;
//        for (int i = 0; i < texts.size(); i++) {
//            String text = texts.get(i);
//            TtsConfig textCfg = new TtsConfig();
//            OfflineTts tts = getInstance(textCfg);
//            String wavPath = "/tmp/melo_" + (i + 1) + ".wav";
//            GeneratedAudio audio = tts.generate(text, 0, textCfg.lengthScale);
//            audio.save(wavPath);
//            log.info("[MeloTts]       {} Hz, {} samples", audio.getSampleRate(), audio.getSamples().length);
//
//
//
//            if (textCfg.playAfterSynthesize) {
//                playAudio(audio);
//            }
//        }


        OfflineTts tts = MeloTtsUtil.getInstance();
        String ttsPath = "/tmp/melo" + File.separator + new Snowflake().nextIdStr() + ".wav";
        GeneratedAudio audio = tts.generate("你好", 0, 1);
        audio.save(ttsPath);
        playAudio(audio);
        log.info("[MeloTts] ----------------------------------------");
//        log.info("[MeloTts] 完成! 单字单词 {} 条, 句子 {} 条", wordCount, sentCount);
        log.info("[MeloTts] ========================================");
    }
}
