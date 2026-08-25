package com.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GLM模型配置
 * <p>
 * 配置智谱AI GLM模型的API密钥、模型名称和接口地址
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "glm")
public class GlmConfig {

    /** API密钥 */
    private String apiKey;

    /** 接口地址 */
    private String baseUrl;

    /** 模型名称 */
    private String model;
}
