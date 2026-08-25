package com.agentscope.service.impl;

import com.agentscope.config.EmbeddingConfig;
import com.agentscope.service.EmbeddingCacheService;
import com.agentscope.service.EmbeddingService;
import ai.onnxruntime.*;
import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Embedding服务实现类
 * <p>
 * 使用本地ONNX模型进行文本向量化
 * 基于BGE系列中文嵌入模型
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingConfig config;
    private final EmbeddingCacheService cacheService;
    private OrtEnvironment env;
    private OrtSession session;
    private HuggingFaceTokenizer hfTokenizer;
    private int dimension;

    @PostConstruct
    public void init() {
        try {
            log.info("初始化Embedding模型...");
            log.info("Tokenizer路径: {}", config.getTokenizerUri());
            log.info("ONNX模型路径: {}", config.getOnnxModelUri());

            env = OrtEnvironment.getEnvironment();

            String modelPath = extractPath(config.getOnnxModelUri());
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = env.createSession(modelPath, options);

            String tokenizerPath = extractPath(config.getTokenizerUri());
            hfTokenizer = HuggingFaceTokenizer.newInstance(Paths.get(tokenizerPath));

            dimension = 768;
            log.info("Embedding模型初始化成功，维度: {}", dimension);

        } catch (Exception e) {
            log.error("Embedding模型初始化失败", e);
            throw new RuntimeException("Embedding模型初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
            if (hfTokenizer != null) hfTokenizer.close();
        } catch (Exception e) {
            log.error("关闭Embedding模型资源失败", e);
        }
    }

    private int[] tokenize(String text) {
        Encoding encoding = hfTokenizer.encode(text);
        long[] ids = encoding.getIds();
        int maxLen = Math.min(ids.length, 512);
        int[] inputIds = new int[maxLen];
        for (int i = 0; i < maxLen; i++) {
            inputIds[i] = (int) ids[i];
        }
        return inputIds;
    }

    @Override
    public float[] embed(String text) {
        float[] cached = cacheService.get(text);
        if (cached != null) {
            return cached;
        }

        try {
            int[] inputIds = tokenize(text);
            int seqLen = inputIds.length;

            long[][] inputIdsLong = new long[1][seqLen];
            long[][] attentionMaskLong = new long[1][seqLen];
            long[][] tokenTypeIdsLong = new long[1][seqLen];
            for (int i = 0; i < seqLen; i++) {
                inputIdsLong[0][i] = inputIds[i];
                attentionMaskLong[0][i] = 1;
                tokenTypeIdsLong[0][i] = 0;
            }

            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, inputIdsLong);
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, attentionMaskLong);
            OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(env, tokenTypeIdsLong);

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);
            inputs.put("token_type_ids", tokenTypeIdsTensor);

            OrtSession.Result result = session.run(inputs);
            float[][][] output = (float[][][]) result.get(0).getValue();
            float[] embedding = output[0][0];

            if (config.isNormalize()) {
                embedding = normalize(embedding);
            }

            inputIdsTensor.close();
            attentionMaskTensor.close();
            tokenTypeIdsTensor.close();
            result.close();

            cacheService.put(text, embedding);
            return embedding;

        } catch (Exception e) {
            log.error("文本向量化失败: {}", e.getMessage(), e);
            throw new RuntimeException("文本向量化失败", e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return texts.stream()
                .map(this::embed)
                .collect(Collectors.toList());
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    private float[] normalize(float[] vector) {
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    private String extractPath(String uri) {
        if (uri.startsWith("file:")) {
            return uri.substring(5);
        }
        return uri;
    }
}
