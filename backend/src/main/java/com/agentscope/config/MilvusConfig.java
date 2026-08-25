package com.agentscope.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Milvus配置
 * 从application.yml中读取Milvus相关配置
 * 仅在 milvus.enabled=true 时创建客户端
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
@ConditionalOnProperty(prefix = "milvus", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MilvusConfig {

    /** Milvus服务地址 */
    private String host = "localhost";

    /** Milvus服务端口 */
    private int port = 19530;

    /** Collection名称前缀 */
    private String collectionPrefix = "kb_";

    /** 向量维度 */
    private int dimension = 512;

    /**
     * 创建Milvus客户端Bean（懒加载，延迟到首次使用时连接）
     */
    @Bean
    @Lazy
    public MilvusClientV2 milvusClient() {
        log.info("初始化Milvus客户端连接: {}:{}", host, port);
        ConnectConfig connectConfig = ConnectConfig.builder()
                .uri(String.format("http://%s:%d", host, port))
                .connectTimeoutMs(30000)
                .build();
        return new MilvusClientV2(connectConfig);
    }
}
