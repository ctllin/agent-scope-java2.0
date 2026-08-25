package com.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 快捷登录配置
 * <p>
 * 从application.yml中读取快捷登录相关配置
 * 注意：此功能仅在开发环境启用，生产环境必须关闭
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.quick-login")
public class QuickLoginConfig {

    /** 是否启用快捷登录 */
    private boolean enabled;
    
    /** 快捷登录用户名 */
    private String username;
    
    /** 快捷登录密码 */
    private String password;
}
