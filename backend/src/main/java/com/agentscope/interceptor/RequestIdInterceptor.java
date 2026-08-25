package com.agentscope.interceptor;

import com.agentscope.common.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 请求日志拦截器。
 * <p>
 * 职责：
 * <ol>
 *   <li>兜底确保MDC中存在traceId（正常情况已由TraceContextFilter写入）</li>
 *   <li>preHandle打印请求入口日志：方法、地址、查询参数</li>
 *   <li>afterCompletion打印请求出口日志：HTTP状态、耗时、JSON请求体参数
 *       （body在控制器读取后才有缓存内容，因此必须在afterCompletion阶段输出）</li>
 * </ol>
 * 日志均自动携带traceId（由logback pattern的%X{traceId}输出）。
 */
@Slf4j
@Component
public class RequestIdInterceptor implements HandlerInterceptor {

    /**
     * MDC中的链路追踪键，与logback pattern、线程池装饰器保持一致
     */
    public static final String TRACE_ID_KEY = "traceId";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * ThreadLocal记录请求开始时间（拦截器实例单例，需按线程隔离）
     */
    private static final ThreadLocal<Long> START_AT = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        START_AT.set(System.currentTimeMillis());

        // 兜底：确保MDC存在traceId（filter已写，此处防御性补充）
        if (!StringUtils.hasText(MDC.get(TRACE_ID_KEY))) {
            String traceId = RequestContext.getRequestId();
            MDC.put(TRACE_ID_KEY, traceId);
        }

        // 入口日志：方法 + 地址 + 查询参数
        String query = request.getQueryString();
        log.info("==> {} {}{}", request.getMethod(), request.getRequestURI(),
                query != null ? "?" + query : "");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            Long start = START_AT.get();
            long costMs = start != null ? System.currentTimeMillis() - start : -1;

            // 出口日志：状态 + 耗时；JSON请求体在此阶段已可从缓存读取
            String bodyLog = extractJsonBody(request);
            log.info("<== {} {} -> {} ({}ms){}", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), costMs, bodyLog);

            if (ex != null) {
                log.error("请求处理异常: {}", request.getRequestURI(), ex);
            }
        } finally {
            START_AT.remove();
        }
    }

    /**
     * 从缓存包装器提取JSON请求体为单行字符串用于日志。
     * 仅处理application/json且非空的内容；过长截断至2KB防止日志膨胀；
     * 尝试格式化为紧凑JSON失败则原样输出。
     */
    private String extractJsonBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return "";
        }
        String contentType = request.getContentType();
        if (contentType == null || !contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return "";
        }
        byte[] buf = wrapper.getContentAsByteArray();
        if (buf.length == 0) {
            return "";
        }
        String body = new String(buf, StandardCharsets.UTF_8).trim();
        if (body.isEmpty()) {
            return "";
        }
        if (body.length() > 2048) {
            body = body.substring(0, 2048) + "...(截断)";
            return " body=" + body;
        }
        // 紧凑化输出（去除换行缩进），失败则原样返回
        try {
            Object json = OBJECT_MAPPER.readValue(body, Object.class);
            return " body=" + OBJECT_MAPPER.writeValueAsString(json);
        } catch (Exception e) {
            return " body=" + body;
        }
    }

    /**
     * 表单参数格式化（当前未启用表单日志，保留工具方法供扩展）
     */
    @SuppressWarnings("unused")
    private String formatParams(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if (params.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" params={");
        params.forEach((k, v) -> sb.append(k).append('=').append(String.join(",", v)).append(','));
        return sb.append('}').toString();
    }
}
