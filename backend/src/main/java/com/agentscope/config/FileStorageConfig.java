package com.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageConfig {

    /** 文件存储基础路径 */
    private String basePath = "/data/agent-scope/files";
}
