package com.agentscope.util;

import com.benjaminwan.ocrlibrary.OcrResult;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.InferenceEngine;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
public class OcrUtil {

    /** RapidOCR 库默认解压目录 */
    private static final String DEFAULT_OCR_DIR = "/tmp/ocrJava/onnx";
    public static final InferenceEngine engine = getEngine();

    private static InferenceEngine getEngine() {
        Path soPath = Paths.get(DEFAULT_OCR_DIR, "libRapidOcr.so");
        // 1. 确保 libRapidOcr.so 存在（从 jar 提取）
        try {
            extractIfAbsent(soPath, "/lib/libRapidOcr.so");
            // 2. 修复 executable stack（RWE → RW）
            fixExecutableStack(soPath);
            // 3. 执行 OCR
            return InferenceEngine.getInstance(Model.ONNX_PPOCR_V4);

        } catch (Exception e) {
            log.error("extractIfAbsent.error", e);
            return null;
        }

    }

    public static void main(String[] args) throws Exception {
        String imagePath = args.length > 0 ? args[0] : "/home/ctl/Pictures/简历.jpg";
        OcrResult result = engine.runOcr(imagePath);
        System.out.println("识别文本：" + result.getStrRes().trim());
    }

    /** 若目标文件不存在，从 classpath 资源提取 */
    private static void extractIfAbsent(Path target, String resource) throws IOException {
        if (Files.exists(target)) {
            System.out.println("已存在: " + target);
            return;
        }
        Files.createDirectories(target.getParent());
        try (InputStream is = OcrUtil.class.getResourceAsStream(resource)) {
            if (is == null) {
                System.err.println("资源不存在: " + resource);
                return;
            }
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("已提取: " + target);
    }

    /**
     * 修复 ELF PT_GNU_STACK executable stack 标记。
     * 将 flags 从 RWE(0x7) 改为 RW(0x6)，移除 PF_X(0x1)。
     */
    private static void fixExecutableStack(Path soPath) throws IOException {
        byte[] data = Files.readAllBytes(soPath);
        if (data.length < 64) {
            System.out.println("文件过小，跳过修复");
            return;
        }
        if (data[0] != 0x7f || data[1] != 'E' || data[2] != 'L' || data[3] != 'F') {
            System.out.println("不是 ELF 文件，跳过修复");
            return;
        }

        boolean is64 = data[4] == 2;
        long phoff = is64 ? readLELong(data, 32) : readLEInt(data, 24);
        int phentsize = is64 ? readLEShort(data, 54) : readLEShort(data, 42);
        int phnum = is64 ? readLEShort(data, 56) : readLEShort(data, 44);
        int flagsOffset = is64 ? 4 : 24;

        int PT_GNU_STACK = 0x6474e551;
        for (int i = 0; i < phnum; i++) {
            long pos = phoff + (long) i * phentsize;
            if (pos + 4 > data.length) break;
            int ptype = readLEInt(data, (int) pos);
            if (ptype == PT_GNU_STACK) {
                int fPos = (int) pos + flagsOffset;
                int old = data[fPos] & 0xFF;
                if ((old & 0x1) == 0) {
                    System.out.println("已是 RW，无需修复");
                    return;
                }
                data[fPos] = (byte) (old & ~0x1);
                Files.write(soPath, data);
                System.out.printf("execstack 修复: 0x%x → 0x%x%n", old, old & ~0x1);
                return;
            }
        }
        System.out.println("未找到 PT_GNU_STACK");
    }

    private static int readLEInt(byte[] d, int o) {
        return (d[o] & 0xFF) | ((d[o+1] & 0xFF) << 8)
                | ((d[o+2] & 0xFF) << 16) | ((d[o+3] & 0xFF) << 24);
    }

    private static long readLELong(byte[] d, int o) {
        return (long) readLEInt(d, o) & 0xFFFFFFFFL | ((long) readLEInt(d, o+4) << 32);
    }

    private static int readLEShort(byte[] d, int o) {
        return (d[o] & 0xFF) | ((d[o+1] & 0xFF) << 8);
    }
}
