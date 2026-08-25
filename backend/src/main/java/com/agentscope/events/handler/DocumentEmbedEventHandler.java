package com.agentscope.events.handler;

import com.agentscope.events.BizEvent;
import com.agentscope.events.BizEventType;
import com.agentscope.events.BizEventHandler;
import com.agentscope.events.BizEventHandlerRegistry;
import com.agentscope.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 文档向量化事件处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEmbedEventHandler implements BizEventHandler {

    private final KnowledgeBaseService knowledgeBaseService;

    private final BizEventHandlerRegistry registry;

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public BizEventType type() {
        return BizEventType.DOCUMENT_EMBED;
    }

    @Override
    public void handle(BizEvent event) {
        String documentId = event.getBizId();
        try {
            knowledgeBaseService.reembedDocument(documentId);
            log.info("文档向量化完成: documentId={}", documentId);
        } catch (Exception e) {
            // 失败语义与原实现一致：分块保持未嵌入状态，可再次手动触发
            log.error("文档向量化失败: documentId={}", documentId, e);
        }
    }
}
