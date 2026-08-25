package com.agentscope.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agentscope.model.entity.AsrRecord;
import com.agentscope.service.AsrService;
import com.agentscope.util.VoskAsrUtil;
import lombok.extern.slf4j.Slf4j;
import org.vosk.Recognizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时语音识别 WebSocket 处理器
 * <p>
 * 协议：
 * - 文本消息（控制）：{"action":"start","mode":"browser|server","recordId":"可选，传入则覆盖该记录"}
 * - 二进制消息（仅browser模式）：16kHz 单声道 16bit LE PCM 裸流
 * - 服务端推送：{"event":"started"} / {"event":"partial","text":..}
 *              / {"event":"result","text":..}（完整句子）/ {"event":"final","text":..,"recordId":..}
 * </p>
 */
@Slf4j
@Component
public class AsrWebSocketHandler extends AbstractWebSocketHandler {

    @Autowired
    private AsrService asrService;

    /** 会话状态表 */
    private final Map<String, SessionState> states = new ConcurrentHashMap<>();

    private static class SessionState {
        volatile boolean running = false;
        String mode = "browser";
        String recordId;
        String lang = VoskAsrUtil.DEFAULT_LANG;
        long startAtMillis;
        final StringBuilder transcript = new StringBuilder();
        final java.io.ByteArrayOutputStream pcmBuffer = new java.io.ByteArrayOutputStream();
        Recognizer recognizer;
        Thread micThread;
        TargetDataLine micLine;
        long lastPartialSentAt = 0;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JSONObject req;
        try {
            req = JSONUtil.parseObj(message.getPayload());
        } catch (Exception e) {
            send(session, new JSONObject().set("event", "error").set("message", "非法指令"));
            return;
        }
        String action = req.getStr("action", "");
        switch (action) {
            case "start" -> handleStart(session, req);
            case "stop" -> handleStop(session);
            default -> send(session, new JSONObject().set("event", "error").set("message", "未知action: " + action));
        }
    }

    private synchronized void handleStart(WebSocketSession session, JSONObject req) {
        SessionState st = states.computeIfAbsent(session.getId(), k -> new SessionState());
        if (st.running) {
            send(session, new JSONObject().set("event", "error").set("message", "识别已在进行中"));
            return;
        }
        try {
            st.mode = req.getStr("mode", "browser");
            st.recordId = req.getStr("recordId", null);
            st.lang = VoskAsrUtil.normalizeLang(req.getStr("lang", VoskAsrUtil.DEFAULT_LANG));
            st.transcript.setLength(0);
            st.startAtMillis = System.currentTimeMillis();
            st.recognizer = VoskAsrUtil.createRecognizer(st.lang);
            st.running = true;

            if ("server".equals(st.mode)) {
                startServerMic(st, session);
            }
            send(session, new JSONObject().set("event", "started"));
        } catch (Exception e) {
            st.running = false;
            cleanup(st);
            send(session, new JSONObject().set("event", "error")
                    .set("message", "启动识别失败: " + e.getMessage()));
        }
    }

    /** 服务器本机麦克风采集线程 */
    private void startServerMic(SessionState st, WebSocketSession session) throws Exception {
        AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("服务器未找到可用麦克风设备");
        }
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        st.micLine = line;

        st.micThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (st.running && session.isOpen()) {
                int n = line.read(buffer, 0, buffer.length);
                if (n <= 0) break;
                feedAudio(st, session, buffer, n);
            }
        }, "asr-server-mic-" + session.getId());
        st.micThread.setDaemon(true);
        st.micThread.start();
    }

    /** browser模式：接收浏览器发来的PCM块 */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        SessionState st = states.get(session.getId());
        if (st == null || !st.running || !"browser".equals(st.mode)) return;
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        feedAudio(st, session, bytes, bytes.length);
    }

    private void feedAudio(SessionState st, WebSocketSession session, byte[] buffer, int length) {
        Recognizer recognizer = st.recognizer;
        if (recognizer == null || !session.isOpen()) return;

        // 累积原始PCM用于会话结束后生成录音文件
        synchronized (st.pcmBuffer) {
            st.pcmBuffer.write(buffer, 0, length);
        }

        boolean hasFullResult;
        String jsonResult;
        synchronized (this) {
            hasFullResult = recognizer.acceptWaveForm(buffer, length);
            jsonResult = hasFullResult ? recognizer.getResult() : recognizer.getPartialResult();
        }

        if (jsonResult == null) return;
        JSONObject obj;
        try {
            obj = JSONUtil.parseObj(jsonResult);
        } catch (Exception e) {
            return;
        }

        if (hasFullResult) {
            String text = obj.getStr("text", "");
            if (!text.isBlank()) {
                if (st.transcript.length() > 0) st.transcript.append(' ');
                st.transcript.append(text.trim());
                send(session, new JSONObject().set("event", "result").set("text", text));
            }
        } else {
            // 部分结果节流推送（300ms），避免刷屏
            long now = System.currentTimeMillis();
            String partial = obj.getStr("partial", "");
            if (!partial.isBlank() && now - st.lastPartialSentAt > 300) {
                st.lastPartialSentAt = now;
                send(session, new JSONObject().set("event", "partial").set("text", partial));
            }
        }
    }

    private void handleStop(WebSocketSession session) {
        SessionState st = states.get(session.getId());
        if (st == null) return;
        st.running = false;

        try {
            Thread.sleep(150); // 等待麦克风读循环退出当前read
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        double durationSec = Math.max(0, (System.currentTimeMillis() - st.startAtMillis) / 1000.0);

        // 收尾：冲刷剩余未提交音频，合并最终结果
        if (st.recognizer != null) {
            String tailJson = null;
            synchronized (this) {
                try {
                    tailJson = st.recognizer.getFinalResult();
                } catch (Exception ignored) {
                }
            }
            if (tailJson != null) {
                try {
                    String tail = JSONUtil.parseObj(tailJson).getStr("text", "");
                    if (!tail.isBlank()) {
                        if (st.transcript.length() > 0) st.transcript.append(' ');
                        st.transcript.append(tail.trim());
                    }
                } catch (Exception ignored) {
                }
            }
        }
        cleanup(st);

        String finalText = st.transcript.toString().trim();

        // 持久化实时识别结果（传了recordId则覆盖原记录）
        String savedId = null;
        boolean silent = false;
        try {
            byte[] pcm;
            synchronized (st.pcmBuffer) {
                pcm = st.pcmBuffer.toByteArray();
                st.pcmBuffer.reset();
            }
            // 判定本次会话是否基本无声（int16峰值<50≈0.15%量程）
            int peak = 0;
            for (int i = 0; i + 1 < pcm.length; i += 2) {
                short s = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
                int v = Math.abs(s);
                if (v > peak) peak = v;
            }
            silent = peak < 50;

            AsrRecord saved = asrService.saveRealtimeResult(
                    st.recordId, null, finalText, (long) durationSec, st.lang);
            savedId = saved.getId();

            // 会话音频落盘（16k单声道16bit WAV），供历史回放
            if (pcm.length > 0) {
                asrService.attachAudio(savedId, buildWav(pcm));
            }
        } catch (Exception e) {
            log.error("保存实时识别结果失败", e);
        }

        JSONObject resp = new JSONObject()
                .set("event", "final")
                .set("text", finalText)
                .set("duration", durationSec)
                .set("recordId", savedId)
                .set("silent", silent);
        send(session, resp);
        states.remove(session.getId());
    }

    private void cleanup(SessionState st) {
        if (st.micLine != null) {
            try {
                st.micLine.stop();
                st.micLine.close();
            } catch (Exception ignored) {
            }
            st.micLine = null;
        }
        if (st.recognizer != null) {
            try {
                st.recognizer.close();
            } catch (Exception ignored) {
            }
            st.recognizer = null;
        }
    }

    /**
     * 将裸PCM(16kHz单声道16bit LE)包装为标准WAV文件字节
     */
    private static byte[] buildWav(byte[] pcm) {
        int sampleRate = 16000;
        int channels = 1;
        int bitsPerSample = 16;
        int blockAlign = channels * bitsPerSample / 8;
        int byteRate = sampleRate * blockAlign;
        int totalLen = 36 + pcm.length;

        java.nio.ByteBuffer wav = java.nio.ByteBuffer.allocate(44 + pcm.length);
        wav.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes());
        wav.putInt(totalLen);
        wav.put("WAVE".getBytes());
        wav.put("fmt ".getBytes());
        wav.putInt(16);                    // PCM块大小
        wav.putShort((short) 1);           // PCM格式
        wav.putShort((short) channels);
        wav.putInt(sampleRate);
        wav.putInt(byteRate);
        wav.putShort((short) blockAlign);
        wav.putShort((short) bitsPerSample);
        wav.put("data".getBytes());
        wav.putInt(pcm.length);
        wav.put(pcm);
        return wav.array();
    }

    private void send(WebSocketSession session, JSONObject json) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json.toString()));
            }
        } catch (Exception e) {
            log.warn("WebSocket发送失败: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionState st = states.remove(session.getId());
        if (st != null) {
            st.running = false;
            cleanup(st);
        }
    }
}
