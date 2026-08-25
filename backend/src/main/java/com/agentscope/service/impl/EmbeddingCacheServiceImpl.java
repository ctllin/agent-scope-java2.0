package com.agentscope.service.impl;

import com.agentscope.service.EmbeddingCacheService;

import com.agentscope.model.entity.EmbeddingCache;
import com.agentscope.repository.EmbeddingCacheRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Embedding向量缓存服务
 * 三级缓存：Caffeine（JVM内存） -> MongoDB（持久化） -> 重新计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingCacheServiceImpl implements EmbeddingCacheService {

    private final EmbeddingCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    /** Caffeine本地缓存：最多10000条，写入后5分钟过期 */
    private Cache<String, float[]> localCache;

    @PostConstruct
    public void init() {
        localCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
        log.info("Embedding缓存服务初始化完成，Caffeine缓存容量: 10000");
    }

    /**
     * 获取向量：Caffeine -> MongoDB -> null
     */
    public float[] get(String text) {
        String hash = md5(text);

        // 1. 查Caffeine
        float[] cached = localCache.getIfPresent(hash);
        if (cached != null) {
            log.debug("Embedding缓存命中(Caffeine): text={}", truncate(text));
            return cached;
        }

        // 2. 查MongoDB
        Optional<EmbeddingCache> dbCache = cacheRepository.findByTextHash(hash);
        if (dbCache.isPresent()) {
            float[] vector = parseVector(dbCache.get().getVectorJson());
            if (vector != null) {
                localCache.put(hash, vector);
                log.debug("Embedding缓存命中(MongoDB): text={}", truncate(text));
                return vector;
            }
        }

        return null;
    }

    /**
     * 存入缓存（Caffeine + MongoDB）
     */
    public void put(String text, float[] vector) {
        String hash = md5(text);
        String vectorJson = serializeVector(vector);

        // 存Caffeine
        localCache.put(hash, vector);

        // 存MongoDB（忽略重复）
        try {
            if (!cacheRepository.findByTextHash(hash).isPresent()) {
                EmbeddingCache cache = EmbeddingCache.builder()
                        .textHash(hash)
                        .text(text)
                        .vectorJson(vectorJson)
                        .dimension(vector.length)
                        .build();
                cacheRepository.save(cache);
                log.debug("Embedding缓存已存入: text={}, dimension={}", truncate(text), vector.length);
            }
        } catch (Exception e) {
            log.warn("Embedding缓存写入MongoDB失败: {}", e.getMessage());
        }
    }

    /**
     * 批量获取
     */
    public List<float[]> getBatch(List<String> texts) {
        return texts.stream().map(this::get).collect(Collectors.toList());
    }

    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }

    private String serializeVector(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private float[] parseVector(String json) {
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String text) {
        return text.length() > 50 ? text.substring(0, 50) + "..." : text;
    }
}
