package com.agentscope.controller;

import com.agentscope.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    @PostMapping("/speak")
    public ResponseEntity<byte[]> speak(@RequestBody Map<String, String> request) {
        String text = request.getOrDefault("text", "").trim();
        String engine = request.getOrDefault("engine", "edge");

        if (text.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }

        TtsService.TtsResult result = ttsService.speak(text, engine);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(result.audio());
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache() {
        ttsService.clearCache();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> cacheStats() {
        return ResponseEntity.ok(ttsService.getCacheStats());
    }
}
