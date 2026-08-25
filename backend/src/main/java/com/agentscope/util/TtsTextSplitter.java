package com.agentscope.util;

import com.agentscope.model.entity.TtsSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * 语音合成文本切分器。
 * <p>
 * 三种模式均产出"分段列表"，每段携带其在原文中的字符偏移：
 * <ul>
 *   <li>LINE：按换行切分，每行一段</li>
 *   <li>PARAGRAPH：按空行（含空白行）切分，每段一个段落</li>
 *   <li>ALL：整篇作为整体</li>
 * </ul>
 * 任何超过 {@link #MAX_CHUNK} 字的单元会在句号/问号等边界处二次切分，
 * 保证每段长度满足TTS接口限制；偏移始终基于原文坐标，供前端高亮映射。
 */
public class TtsTextSplitter {

    /** 单段最大字符数（低于接口500字上限，留余量） */
    public static final int MAX_CHUNK = 450;

    /** 二次切分时句内达到该长度后遇到断句符即收束，避免段落被切碎 */
    private static final int MIN_SENTENCE = 60;

    private TtsTextSplitter() {
    }

    /**
     * 按模式切分文本
     *
     * @param text 原始全文
     * @param mode LINE/PARAGRAPH/ALL
     * @return 分段列表（仅含text与偏移），空行自动跳过；无有效内容返回空列表
     */
    public static List<TtsSegment> split(String text, String mode) {
        List<TtsSegment> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        switch (mode == null ? "" : mode) {
            case "LINE" -> splitByLine(text, result);
            case "PARAGRAPH" -> splitByParagraph(text, result);
            default -> addUnit(text, 0, text.length(), result);
        }
        return result;
    }

    /** 按行切分：每个非空行为一个单元 */
    private static void splitByLine(String text, List<TtsSegment> out) {
        int start = 0;
        while (start <= text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) end = text.length();
            addUnit(text, start, end, out);
            if (end == text.length()) break;
            start = end + 1;
        }
    }

    /** 按段落切分：以空行（仅含空白字符的行）为界 */
    private static void splitByParagraph(String text, List<TtsSegment> out) {
        int paraStart = -1;
        int lineStart = 0;
        int len = text.length();
        for (int i = 0; i <= len; i++) {
            boolean eol = i == len || text.charAt(i) == '\n';
            if (!eol) continue;
            String line = text.substring(lineStart, i);
            if (line.isBlank()) {
                if (paraStart >= 0) {
                    addUnit(text, paraStart, lastNonBlankEnd(text, paraStart, lineStart), out);
                    paraStart = -1;
                }
            } else if (paraStart < 0) {
                paraStart = lineStart;
            }
            if (i == len) break;
            lineStart = i + 1;
        }
        if (paraStart >= 0) {
            addUnit(text, paraStart, lastNonBlankEnd(text, paraStart, len), out);
        }
    }

    /** 找到 [from,to) 范围内最后一个非空白字符之后的位置 */
    private static int lastNonBlankEnd(String text, int from, int to) {
        int end = to;
        while (end > from && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return Math.max(end, from);
    }

    /**
     * 处理一个原始单元：去除首尾空白得到有效范围；
     * 长度≤MAX_CHUNK直接成段，否则按断句符二次切分。
     */
    private static void addUnit(String text, int unitStart, int unitEnd, List<TtsSegment> out) {
        int s = unitStart;
        int e = unitEnd;
        while (s < e && Character.isWhitespace(text.charAt(s))) s++;
        while (e > s && Character.isWhitespace(text.charAt(e - 1))) e--;
        if (s >= e) return;

        if (e - s <= MAX_CHUNK) {
            out.add(TtsSegment.builder()
                    .text(text.substring(s, e))
                    .charStart(s)
                    .charEnd(e)
                    .build());
            return;
        }

        // 超长单元：在句子边界处切分
        int segStart = s;
        for (int i = s; i < e; i++) {
            int segLen = i + 1 - segStart;
            char c = text.charAt(i);
            boolean hardBreak = segLen >= MAX_CHUNK;
            boolean softBreak = isBreakChar(c) && segLen >= MIN_SENTENCE;
            if (hardBreak || softBreak) {
                // 收束时吃掉紧随的引号/括号等闭合符号
                int end = i + 1;
                while (end < e && isCloseChar(text.charAt(end))) end++;
                out.add(buildSeg(text, segStart, end));
                segStart = skipWhitespace(text, end, e);
                if (segStart >= e) break;
            }
        }
        if (segStart < e) {
            // 尾部剩余内容若过短则并入上一段，避免出现单字成段
            if (out.size() > 0 && e - segStart < MIN_SENTENCE / 2) {
                TtsSegment last = out.get(out.size() - 1);
                if (last.getCharEnd() != null && last.getCharEnd() - last.getCharStart() + (e - segStart) <= MAX_CHUNK) {
                    last.setText(text.substring(last.getCharStart(), e));
                    last.setCharEnd(e);
                    return;
                }
            }
            out.add(buildSeg(text, segStart, e));
        }
    }

    private static TtsSegment buildSeg(String text, int start, int end) {
        return TtsSegment.builder()
                .text(text.substring(start, end))
                .charStart(start)
                .charEnd(end)
                .build();
    }

    private static int skipWhitespace(String text, int from, int to) {
        int i = from;
        while (i < to && Character.isWhitespace(text.charAt(i))) i++;
        return i;
    }

    /** 断句符：中英文问号、叹号、分号、冒号、省略号及换行（英文句号不切，避免小数/缩写误断） */
    private static boolean isBreakChar(char c) {
        return c == '。' || c == '！' || c == '？' || c == '；' || c == '：'
                || c == '!' || c == '?' || c == ';' || c == '\n'
                || c == '…';
    }

    /** 闭合类标点：切分点允许顺延包含 */
    private static boolean isCloseChar(char c) {
        return c == '”' || c == '』' || c == '）' || c == '"' || c == '\'' || c == ')';
    }
}
