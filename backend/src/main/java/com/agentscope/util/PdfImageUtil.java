package com.agentscope.util;

import com.benjaminwan.ocrlibrary.OcrResult;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 转图像工具类 - 基于 Apache PDFBox 的 PDF 页面渲染。
 * <p>
 * 支持将 PDF 文件或输入流转换为 BufferedImage 对象，
 * 也可直接保存为 PNG/JPEG 等常见图像格式。
 *
 * @author chat-bot
 * @see PDFRenderer
 */
public final class PdfImageUtil {

    /**
     * 默认 DPI
     */
    public static final int DEFAULT_DPI = 150;
    /**
     * 最大 DPI 上限，防止 OOM
     */
    public static final int MAX_DPI = 600;
    private static final Logger log = LoggerFactory.getLogger(PdfImageUtil.class);

    private PdfImageUtil() {
    }

    // ==================== to BufferedImage ====================

    /**
     * 将 PDF 文件所有页转为 BufferedImage 列表。
     *
     * @param pdfPath PDF 文件路径
     * @param dpi     分辨率，建议 72~600
     * @return 每页对应一个 BufferedImage
     * @throws IOException 文件读取或渲染失败
     */
    public static List<BufferedImage> toImages(Path pdfPath, int dpi) throws IOException {
        validateDpi(dpi);
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            return renderAll(doc, dpi);
        }
    }

    /**
     * 将 PDF 文件所有页转为 BufferedImage 列表（默认 150 DPI）。
     *
     * @param pdfPath PDF 文件路径
     * @return 每页对应一个 BufferedImage
     * @throws IOException 文件读取或渲染失败
     */
    public static List<BufferedImage> toImages(Path pdfPath) throws IOException {
        return toImages(pdfPath, DEFAULT_DPI);
    }

    /**
     * 将 PDF 文件指定页转为 BufferedImage。
     *
     * @param pdfPath   PDF 文件路径
     * @param pageIndex 页码索引（从 0 开始）
     * @param dpi       分辨率
     * @return 渲染后的 BufferedImage
     * @throws IOException              文件读取或渲染失败
     * @throws IllegalArgumentException 页码超出范围
     */
    public static BufferedImage toImage(Path pdfPath, int pageIndex, int dpi) throws IOException {
        validateDpi(dpi);
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            validatePageIndex(doc, pageIndex);
            PDFRenderer renderer = new PDFRenderer(doc);
            return renderer.renderImageWithDPI(pageIndex, dpi);
        }
    }

    /**
     * 将 PDF 文件指定页转为 BufferedImage（默认 150 DPI）。
     *
     * @param pdfPath   PDF 文件路径
     * @param pageIndex 页码索引（从 0 开始）
     * @return 渲染后的 BufferedImage
     * @throws IOException 文件读取或渲染失败
     */
    public static BufferedImage toImage(Path pdfPath, int pageIndex) throws IOException {
        return toImage(pdfPath, pageIndex, DEFAULT_DPI);
    }

    /**
     * 从输入流加载 PDF 并将所有页转为 BufferedImage 列表。
     *
     * @param inputStream PDF 输入流
     * @param dpi         分辨率
     * @return 每页对应一个 BufferedImage
     * @throws IOException 读取或渲染失败
     */
    public static List<BufferedImage> toImages(InputStream inputStream, int dpi) throws IOException {
        validateDpi(dpi);
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(inputStream))) {
            return renderAll(doc, dpi);
        }
    }

    /**
     * 从输入流加载 PDF 并将所有页转为 BufferedImage 列表（默认 150 DPI）。
     *
     * @param inputStream PDF 输入流
     * @return 每页对应一个 BufferedImage
     * @throws IOException 读取或渲染失败
     */
    public static List<BufferedImage> toImages(InputStream inputStream) throws IOException {
        return toImages(inputStream, DEFAULT_DPI);
    }

    // ==================== save to file ====================

    /**
     * 将 PDF 所有页保存为图像文件到指定目录。
     * <p>
     * 文件命名: {baseName}_page_1.png, {baseName}_page_2.png ...
     *
     * @param pdfPath   PDF 文件路径
     * @param outputDir 输出目录（不存在则自动创建）
     * @param format    图像格式（png / jpeg / jpg / bmp / gif）
     * @return 生成的图像文件路径列表
     * @throws IOException 文件读取或写入失败
     */
    public static List<Path> saveImages(Path pdfPath, Path outputDir, String format) throws IOException {
        Files.createDirectories(outputDir);
        String baseName = getBaseName(pdfPath.getFileName().toString());

        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            List<Path> result = new ArrayList<>();

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DEFAULT_DPI);
                String fileName = baseName + "_page_" + (i + 1) + "." + format;
                Path outputPath = outputDir.resolve(fileName);
                ImageIO.write(image, normalizeFormat(format), outputPath.toFile());
                result.add(outputPath);
                log.debug("已保存第 {} 页 -> {}", i + 1, outputPath);
            }

            log.info("PDF 共 {} 页已保存到 {}", doc.getNumberOfPages(), outputDir);
            return result;
        }
    }

    /**
     * 将 PDF 所有页保存为图像文件到指定目录（默认 150 DPI）。
     *
     * @param pdfPath   PDF 文件路径
     * @param outputDir 输出目录
     * @param format    图像格式
     * @return 生成的图像文件路径列表
     * @throws IOException 文件读取或写入失败
     */
    public static List<Path> saveImages(Path pdfPath, Path outputDir) throws IOException {
        return saveImages(pdfPath, outputDir, "png");
    }

    /**
     * 将 PDF 指定页保存为单个图像文件。
     *
     * @param pdfPath    PDF 文件路径
     * @param outputPath 输出图像文件路径
     * @param pageIndex  页码索引（从 0 开始）
     * @param dpi        分辨率
     * @param format     图像格式（png / jpeg / jpg / bmp / gif）
     * @throws IOException 文件读取或写入失败
     */
    public static void saveImage(Path pdfPath, Path outputPath, int pageIndex, int dpi, String format)
            throws IOException {
        validateDpi(dpi);
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            validatePageIndex(doc, pageIndex);
            PDFRenderer renderer = new PDFRenderer(doc);
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi);

            if (outputPath.getParent() != null) {
                Files.createDirectories(outputPath.getParent());
            }
            ImageIO.write(image, normalizeFormat(format), outputPath.toFile());
            log.info("PDF 第 {} 页已保存 -> {}", pageIndex + 1, outputPath);
        }
    }

    /**
     * 将 PDF 指定页保存为单个图像文件（默认 150 DPI）。
     *
     * @param pdfPath    PDF 文件路径
     * @param outputPath 输出图像文件路径
     * @param pageIndex  页码索引（从 0 开始）
     * @param format     图像格式
     * @throws IOException 文件读取或写入失败
     */
    public static void saveImage(Path pdfPath, Path outputPath, int pageIndex, String format) throws IOException {
        saveImage(pdfPath, outputPath, pageIndex, DEFAULT_DPI, format);
    }

    /**
     * 将 BufferedImage 写入输出流。
     *
     * @param image        要写入的图像
     * @param outputStream 输出流
     * @param format       图像格式
     * @throws IOException 写入失败
     */
    public static void writeImage(BufferedImage image, OutputStream outputStream, String format) throws IOException {
        ImageIO.write(image, normalizeFormat(format), outputStream);
    }

    // ==================== info ====================

    /**
     * 获取 PDF 文件的页数。
     *
     * @param pdfPath PDF 文件路径
     * @return 页数
     * @throws IOException 文件读取失败
     */
    public static int getPageCount(Path pdfPath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            return doc.getNumberOfPages();
        }
    }

    /**
     * 获取 PDF 输入流的页数。
     *
     * @param inputStream PDF 输入流
     * @return 页数
     * @throws IOException 读取失败
     */
    public static int getPageCount(InputStream inputStream) throws IOException {
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(inputStream))) {
            return doc.getNumberOfPages();
        }
    }

    // ==================== internal ====================

    private static List<BufferedImage> renderAll(PDDocument doc, int dpi) throws IOException {
        PDFRenderer renderer = new PDFRenderer(doc);
        int pageCount = doc.getNumberOfPages();
        List<BufferedImage> images = new ArrayList<>(pageCount);

        for (int i = 0; i < pageCount; i++) {
            images.add(renderer.renderImageWithDPI(i, dpi));
            log.debug("已渲染第 {}/{} 页", i + 1, pageCount);
        }

        log.info("PDF 共 {} 页已渲染完成 (DPI={})", pageCount, dpi);
        return images;
    }

    private static void validateDpi(int dpi) {
        if (dpi < 1 || dpi > MAX_DPI) {
            throw new IllegalArgumentException("DPI 必须在 1 ~ " + MAX_DPI + " 之间，当前值: " + dpi);
        }
    }

    private static void validatePageIndex(PDDocument doc, int pageIndex) {
        if (pageIndex < 0 || pageIndex >= doc.getNumberOfPages()) {
            throw new IllegalArgumentException(
                    "页码索引超出范围: " + pageIndex + "，有效范围: 0 ~ " + (doc.getNumberOfPages() - 1));
        }
    }

    private static String getBaseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String normalizeFormat(String format) {
        if (format == null) {
            return "png";
        }
        String lower = format.toLowerCase().trim();
        if ("jpg".equals(lower)) {
            return "jpeg";
        }
        return lower;
    }

    public static void main(String[] args) throws IOException {
        //PDF
        List<Path> paths = saveImages(Path.of("/home/ctl/Pictures/", "81.pdf"), Path.of("/home/ctl/Pictures/", "81"));
        paths.forEach(path -> {
            assert OcrUtil.engine != null;
            OcrResult ocrResult = OcrUtil.engine.runOcr(path.toString());
            System.out.println(ocrResult.getStrRes());
            ;
            ;
        });
    }
}
