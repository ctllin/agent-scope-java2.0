package com.agentscope.config;

import com.agentscope.service.impl.AsrWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置（实时语音识别）
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class AsrWebSocketConfig implements WebSocketConfigurer {

    private final AsrWebSocketHandler asrWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(asrWebSocketHandler, "/api/asr/ws")
                .setAllowedOriginPatterns("*");
    }
}
