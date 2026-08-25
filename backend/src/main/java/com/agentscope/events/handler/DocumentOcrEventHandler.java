package com.agentscope.events.handler;

import cn.hutool.core.util.StrUtil;
import com.agentscope.common.enums.OcrStatus;
import com.agentscope.events.BizEvent;
import com.agentscope.events.BizEventType;
import com.agentscope.events.BizEventHandler;
import com.agentscope.events.BizEventHandlerRegistry;
import com.agentscope.model.entity.KnowledgeDocument;
import com.agentscope.model.entity.KnowledgeBase;
import com.agentscope.service.KnowledgeBaseService;
import com.agentscope.service.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 文档OCR识别事件处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentOcrEventHandler implements BizEventHandler {

    private final OcrService ocrService;
    private final KnowledgeBaseService knowledgeBaseService;

    private final BizEventHandlerRegistry registry;

    @PostConstruct
    public void register() {
        registry.register(this);
    }

    @Override
    public BizEventType type() {
        return BizEventType.DOCUMENT_OCR;
    }

    @Override
    public void handle(BizEvent event) {
        String documentId = event.getBizId();
        KnowledgeDocument doc = knowledgeBaseService.getDocumentById(documentId);
        if (doc == null) {
            log.warn("OCR事件对应文档不存在: {}", documentId);
            return;
        }
        try {
            var pageContents = ocrService.ocrEntireDocument(doc);
            doc.setPageContents(pageContents);
            doc.setOcrStatus(pageContents.isEmpty() ? OcrStatus.FAILED : OcrStatus.DONE);
            knowledgeBaseService.updateDocument(doc);
        } catch (Exception e) {
            log.error("文档OCR失败: documentId={}", documentId, e);
            doc.setOcrStatus(OcrStatus.FAILED);
            knowledgeBaseService.updateDocument(doc);
        }
    }
}
