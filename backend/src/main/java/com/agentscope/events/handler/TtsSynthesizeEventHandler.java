package com.agentscope.events.handler;

import com.agentscope.events.BizEvent;
import com.agentscope.events.BizEventType;
import com.agentscope.events.BizEventHandler;
import com.agentscope.events.BizEventHandlerRegistry;
import com.agentscope.service.TtsRecordService;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 语音合成事件处理
 */
@Component
@RequiredArgsConstructor
public class TtsSynthesizeEventHandler implements BizEventHandler {

    private final TtsRecordService ttsRecordService;

    private final BizEventHandlerRegistry registry;

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public BizEventType type() {
        return BizEventType.TTS_SYNTHESIZE;
    }

    @Override
    public void handle(BizEvent event) {
        ttsRecordService.synthesizeNow(event.getBizId());
    }
}
