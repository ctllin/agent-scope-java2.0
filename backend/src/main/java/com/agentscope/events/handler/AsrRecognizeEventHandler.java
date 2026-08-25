package com.agentscope.events.handler;

import com.agentscope.events.BizEvent;
import com.agentscope.events.BizEventType;
import com.agentscope.events.BizEventHandler;
import com.agentscope.events.BizEventHandlerRegistry;
import com.agentscope.service.AsrService;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 语音识别（文件）事件处理
 */
@Component
@RequiredArgsConstructor
public class AsrRecognizeEventHandler implements BizEventHandler {

    private final AsrService asrService;

    private final BizEventHandlerRegistry registry;

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public BizEventType type() {
        return BizEventType.ASR_RECOGNIZE;
    }

    @Override
    public void handle(BizEvent event) {
        String lang = event.getParams() != null ? event.getParams().getOrDefault("lang", "cn") : "cn";
        asrService.recognizeNow(event.getBizId(), lang);
    }
}
