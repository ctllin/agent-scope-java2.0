package com.agentscope.util;

import com.agentscope.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT工具类
 * <p>
 * 提供JWT令牌的生成和解析功能
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
    }

    /**
     * 生成JWT令牌
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return JWT令牌
     */
    public String generateToken(String userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtConfig.getExpiration());

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌中获取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    /**
     * 解析令牌
     *
     * @param token JWT令牌
     * @return 令牌内容
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证令牌是否有效
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        return !claims.getExpiration().before(new Date());
    }
}
