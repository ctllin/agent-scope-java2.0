package com.agentscope.service.document.impl;

import com.agentscope.service.document.FileParseService;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件解析服务，支持多种文件格式的文本提取
 * 支持 PDF、Word(docx)、TXT 格式
 */
@Slf4j
@Service
public class FileParseServiceImpl implements FileParseService {

    /**
     * 根据文件类型自动选择解析器提取文本内容
     */
    public String parseFile(File file, String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> parsePdf(file);
            case "docx" -> parseDocx(file);
            case "txt" -> parseTxt(file);
            default -> throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        };
    }

    /**
     * 按页提取文本内容，返回每页文本的列表。
     * PDF/DOCX 按原生页面返回，TXT 返回单元素列表。
     * 某页无内容时对应位置为空字符串。
     */
    public List<String> parseFileByPage(File file, String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> parsePdfByPage(file);
            case "docx" -> parseDocxByPage(file);
            case "txt" -> List.of(parseTxt(file));
            default -> throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        };
    }

    /**
     * PDF 按页提取文本
     */
    private List<String> parsePdfByPage(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            int pageCount = document.getNumberOfPages();
            List<String> pages = new ArrayList<>(pageCount);
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= pageCount; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(document);
                pages.add(text != null ? text.trim() : "");
            }
            log.info("PDF按页解析完成: file={}, pages={}", file.getName(), pageCount);
            return pages;
        } catch (IOException e) {
            log.error("PDF按页解析失败: file={}", file.getName(), e);
            throw new RuntimeException("PDF按页解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * DOCX 按段落分组模拟分页（约40段一页）
     */
    private List<String> parseDocxByPage(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            List<String> pages = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            int paraCount = 0;
            int linesPerPage = 40;

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (paragraph.getRuns() != null && !paragraph.getText().trim().isEmpty()) {
                    if (paragraph.getStyleID() != null) {
                        int level = getHeadingLevel(paragraph.getStyleID());
                        if (level > 0) {
                            sb.append("#".repeat(level)).append(" ");
                        }
                    }
                    sb.append(paragraph.getText()).append("\n");
                    paraCount++;
                }
                if (paraCount >= linesPerPage) {
                    pages.add(sb.toString().trim());
                    sb.setLength(0);
                    paraCount = 0;
                }
            }
            String remaining = sb.toString().trim();
            pages.add(remaining.isEmpty() && !pages.isEmpty() ? "" : remaining);

            log.info("DOCX按页解析完成: file={}, pages={}", file.getName(), pages.size());
            return pages;

        } catch (IOException e) {
            log.error("DOCX按页解析失败: file={}", file.getName(), e);
            throw new RuntimeException("DOCX按页解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 PDFBox 提取 PDF 文本
     */
    private String parsePdf(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("PDF解析完成: file={}, pages={}, textLength={}",
                    file.getName(), document.getNumberOfPages(), text.length());
            return text;
        } catch (IOException e) {
            log.error("PDF解析失败: file={}", file.getName(), e);
            throw new RuntimeException("PDF解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 Apache POI 提取 Word 文档文本，保留标题层级标记
     */
    private String parseDocx(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                if (paragraph.getRuns() != null && !paragraph.getText().trim().isEmpty()) {
                    if (paragraph.getStyleID() != null) {
                        int level = getHeadingLevel(paragraph.getStyleID());
                        if (level > 0) {
                            sb.append("#".repeat(level)).append(" ");
                        }
                    }
                    sb.append(paragraph.getText()).append("\n\n");
                }
            }
            String text = sb.toString();
            log.info("DOCX解析完成: file={}, textLength={}", file.getName(), text.length());
            return text;

        } catch (IOException e) {
            log.error("DOCX解析失败: file={}", file.getName(), e);
            throw new RuntimeException("DOCX解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 Word 样式 ID 中提取标题级别
     */
    private int getHeadingLevel(String styleId) {
        if (styleId == null) return 0;
        String lower = styleId.toLowerCase();
        if (lower.contains("heading")) {
            String num = lower.replaceAll("[^0-9]", "");
            if (!num.isEmpty()) return Integer.parseInt(num);
        }
        return 0;
    }

    /**
     * 读取纯文本文件
     */
    private String parseTxt(File file) {
        try {
            String text = Files.readString(file.toPath());
            log.info("TXT解析完成: file={}, textLength={}", file.getName(), text.length());
            return text;
        } catch (IOException e) {
            log.error("TXT解析失败: file={}", file.getName(), e);
            throw new RuntimeException("TXT解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从文件名中提取扩展名
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
