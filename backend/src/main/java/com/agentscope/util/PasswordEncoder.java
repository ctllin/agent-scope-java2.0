package com.agentscope.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 密码工具类
 * <p>
 * 提供密码哈希和验证功能
 * 注意：生产环境应使用BCrypt等更强的哈希算法
 * </p>
 */
public class PasswordEncoder {

    /**
     * 对密码进行哈希处理
     *
     * @param rawPassword 原始密码
     * @return 哈希后的密码
     */
    public static String encode(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 验证密码是否匹配
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 哈希后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
