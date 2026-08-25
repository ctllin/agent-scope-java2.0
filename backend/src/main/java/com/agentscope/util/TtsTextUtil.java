package com.agentscope.util;

public class TtsTextUtil {
    /**
     * 纯数字 0~9
     */
    public static boolean isDigit(String t) {
        return t != null && t.matches("[0-9]");
    }


    /**
     * 单个中文汉字
     */
    public static boolean isChineseSingle(String t) {
        return t != null && t.length() == 1 && t.charAt(0) >= '\u4e00' && t.charAt(0) <= '\u9fff';
    }

    /**
     * 单个英文字母
     */
    public static boolean isEnglishSingle(String t) {
        return t != null && t.length() == 1 && Character.isLetter(t.charAt(0)) && t.charAt(0) < 128;
    }

    /**
     * 是否含中文
     */
    public static boolean hasChinese(String t) {
        return t != null && t.matches(".*[\u4e00-\u9fff].*");
    }

    /**
     * 是否含英文字母
     */
    public static boolean hasEnglish(String t) {
        return t != null && t.matches(".*[a-zA-Z].*");
    }

    /**
     * 是否含数字
     */
    public static boolean hasDigit(String t) {
        return t != null && t.matches(".*[0-9].*");
    }
}
