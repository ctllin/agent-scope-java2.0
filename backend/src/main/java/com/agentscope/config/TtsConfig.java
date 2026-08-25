package com.agentscope.config;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
// ===================== 配置记录 =====================

/**
 * TTS 合成参数配置。使用 Builder 模式，所有参数均有合理默认值。
 * <p>
 * 通过 {@link #builder()} 创建，链式设置需要调整的参数，最后调用 {@code build()}。
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
@ConfigurationProperties(prefix = "tts.sherpa-onnx")
public   class TtsConfig {

    /** 文件存储基础路径 */
    private String path = "/data/agent-scope/files/sherpa-onnx";
    // --- VITS 模型参数 ---
    /** 噪声比例 (0.0~1.0)，控制合成随机性。越大越自然，越小越稳定。默认 0.667 */
    public final float noiseScale=0.667F;
    /** 长噪声比例 (0.0~1.0)，控制语速变化。越大节奏越自然，越小越均匀。默认 0.8 */
    public final float noiseScaleW=0.8F;
    /** 语速缩放 (<1 加速, =1 原速, >1 减速)。默认 1.0 */
    public final float lengthScale=1.0F;

    // --- 模型通用参数 ---
    /** 推理线程数。建议 1~8。默认 2 */
    public final int numThreads=2;
    /** 调试模式。开启后输出详细日志。默认 false */
    public final boolean debug=true;

    // --- 全局配置 ---
    /** 单次合成最大句子数。默认 1 */
    public final int maxNumSentences=1;
    /** 静音比例缩放 (0.0~1.0)，控制句子间停顿。越大停顿越长。默认 0.2 */
    public final float silenceScale=0.2F;
    /** 是否合成后朗读 (仅 main 方法生效)。默认 false */
    public final boolean playAfterSynthesize=true;



    // ===== 文本识别辅助方法 =====

    /**
     * 纯数字 0~9
     */
    public static boolean isDigit(String t) {
        return t != null && t.matches("[0-9]");
    }

    /**
     * 单个中文汉字
     */
    public static boolean isChineseSingle(String t) {
        return t != null && t.length() == 1 && t.charAt(0) >= '\u4e00' && t.charAt(0) <= '\u9fff';
    }

    /**
     * 单个英文字母
     */
    public static boolean isEnglishSingle(String t) {
        return t != null && t.length() == 1 && Character.isLetter(t.charAt(0)) && t.charAt(0) < 128;
    }

    /**
     * 是否含中文
     */
    public static boolean hasChinese(String t) {
        return t != null && t.matches(".*[\u4e00-\u9fff].*");
    }

    /**
     * 是否含英文字母
     */
    public static boolean hasEnglish(String t) {
        return t != null && t.matches(".*[a-zA-Z].*");
    }

    /**
     * 是否含数字
     */
    public static boolean hasDigit(String t) {
        return t != null && t.matches(".*[0-9].*");
    }

}