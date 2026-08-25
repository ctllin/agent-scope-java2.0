package com.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI模型配置
 * <p>
 * 从application.yml中读取AI模型相关配置
 * 支持多模型路由
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.ai-models")
public class AiModelConfig {

    /** AI模型列表 */
    private List<ModelConfig> models;

    /**
     * 单个模型配置
     */
    @Data
    public static class ModelConfig {
        
        /** 模型名称（如：glm-4-flash） */
        private String name;
        
        /** 模型API地址 */
        private String url;
        
        /** 模型标识 */
        private String model;
    }
}
