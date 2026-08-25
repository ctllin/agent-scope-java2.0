package com.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置
 * <p>
 * 从application.yml中读取JWT相关配置
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {

    /** JWT密钥 */
    private String secret;
    
    /** JWT过期时间（毫秒） */
    private Long expiration;
}
