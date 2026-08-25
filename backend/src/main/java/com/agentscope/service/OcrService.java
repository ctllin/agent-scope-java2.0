package com.agentscope.service;

import com.agentscope.model.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * OCR识别服务。
 * <p>
 * 基于PDF转图+RapidOCR离线模型，将文档页面图片识别为文本。
 */
public interface OcrService {

    /** OCR整个文档所有页面，返回按页序排列的内容列表 */
    List<KnowledgeDocument.PageContent> ocrEntireDocument(KnowledgeDocument doc);

    /** OCR指定单页 */
    KnowledgeDocument.PageContent ocrSinglePage(KnowledgeDocument doc, int pageIndex);
}
