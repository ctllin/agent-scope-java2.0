package com.agentscope.service;

import java.util.Map;

public interface TtsService {

    record TtsResult(byte[] audio, String contentType) {}

    TtsResult speak(String text, String engine);

    void clearCache();

    Map<String, Object> getCacheStats();
}
