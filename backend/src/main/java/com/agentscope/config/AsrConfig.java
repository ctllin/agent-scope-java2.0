package com.agentscope.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 语音识别配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.asr")
public class AsrConfig {

    /** 音频文件存储路径 */
    private String audioDir = "/data/agent-scope/asr";

    /** ffmpeg 可执行文件路径 */
    private String ffmpegPath = "ffmpeg";
}
