package com.agentscope.service.impl;

import com.agentscope.service.OcrService;
import com.agentscope.service.XFileStorageService;

import com.agentscope.config.FileStorageConfig;
import com.agentscope.model.entity.KnowledgeDocument;
import com.agentscope.model.entity.KnowledgeDocument.PageContent;
import com.agentscope.util.OcrUtil;
import com.agentscope.util.PdfImageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OcrServiceImpl implements OcrService {
    @Resource
    private FileStorageConfig fileStorageConfig;

    /** 统一文件存储门面：解析文档存储key为绝对路径（兼容历史数据） */
    @Resource
    private XFileStorageService xFileStorage;
    public List<PageContent> ocrEntireDocument(KnowledgeDocument doc) {
        Path filePath = xFileStorage.resolve(doc.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在: " + doc.getFilePath());
        }

        List<PageContent> result = new ArrayList<>();
        try {
            List<BufferedImage> images = PdfImageUtil.toImages(filePath);
            for (int i = 0; i < images.size(); i++) {
                String text = ocrImage(images.get(i), i);
                result.add(PageContent.builder().page(i + 1).text(text).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("PDF OCR失败: " + doc.getName(), e);
        }
        return result;
    }

    public PageContent ocrSinglePage(KnowledgeDocument doc, int pageIndex) {
        Path filePath = xFileStorage.resolve(doc.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("文件不存在: " + doc.getFilePath());
        }

        try {
            BufferedImage image = PdfImageUtil.toImage(filePath, pageIndex);
            String text = ocrImage(image, pageIndex);
            return PageContent.builder().page(pageIndex + 1).text(text).build();
        } catch (Exception e) {
            throw new RuntimeException("PDF OCR失败: " + doc.getName(), e);
        }
    }

    private String ocrImage(BufferedImage image, int pageIndex) {
        if (OcrUtil.engine == null) {
            log.warn("OCR引擎未初始化，跳过第 {} 页", pageIndex + 1);
            return "";
        }
        try {
            Path tempFile = Files.createTempFile("ocr_", ".png");
            try {
                ImageIO.write(image, "png", tempFile.toFile());
                var result = OcrUtil.engine.runOcr(tempFile.toString());
                return result != null ? result.getStrRes().trim() : "";
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception e) {
            log.error("第 {} 页OCR识别失败: {}", pageIndex + 1, e.getMessage());
            return "";
        }
    }
}
