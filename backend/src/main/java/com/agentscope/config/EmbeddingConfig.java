package com.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Embedding配置
 * <p>
 * 从application.yml中读取本地ONNX模型相关配置
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingConfig {

    /** Tokenizer文件路径 */
    private String tokenizerUri;

    /** ONNX模型文件路径 */
    private String onnxModelUri;

    /** 最大序列长度 */
    private int maxSequenceLength = 512;

    /** 是否归一化 */
    private boolean normalize = true;
}
