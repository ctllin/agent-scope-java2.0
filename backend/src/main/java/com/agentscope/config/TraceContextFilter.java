package com.agentscope.config;

import com.agentscope.common.RequestContext;
import com.agentscope.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * 链路追踪上下文过滤器
 * <p>
 * 每次请求时初始化 RequestContext：
 * 1. 从请求头提取或生成 requestId（支持链路追踪）
 * 2. 从JWT令牌提取 userId
 * 3. 请求完成后清理上下文，防止内存泄漏
 * </p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TraceContextFilter extends OncePerRequestFilter {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. 提取或生成 requestId，并同步写入MDC供日志链路追踪输出
            String requestId = request.getHeader(HEADER_REQUEST_ID);
            if (!StringUtils.hasText(requestId)) {
                requestId = RequestContext.generateRequestId();
            }
            RequestContext.setRequestId(requestId);
            MDC.put("traceId", requestId);

            // 2. 从JWT令牌提取 userId
            String userId = extractUserId(request);
            if (StringUtils.hasText(userId)) {
                RequestContext.setUserId(userId);
            }

            // 3. 将 requestId 添加到响应头，方便前端追踪
            response.setHeader(HEADER_REQUEST_ID, requestId);

            log.debug("请求开始 - requestId: {}, userId: {}, uri: {}",
                    requestId, userId, request.getRequestURI());

            // 4. 对JSON请求体做缓存包装（GET/表单无需），供日志拦截器读取请求参数；
            //    包装必须发生在进入业务链之前，否则拦截器拿不到内容
            HttpServletRequest requestToUse = request;
            String contentType = request.getContentType();
            boolean jsonBody = contentType != null && contentType.contains("application/json")
                    && !"GET" .equalsIgnoreCase(request.getMethod());
            if (jsonBody) {
                requestToUse = new ContentCachingRequestWrapper(request);
            }
            filterChain.doFilter(requestToUse, response);

        } finally {
            // 清理MDC，防止线程复用导致的上下文串扰
            MDC.clear();
            // 5. 清理上下文，防止内存泄漏
            log.debug("请求结束 - requestId: {}", RequestContext.getRequestId());
            RequestContext.clear();
        }
    }

    /**
     * 从请求头中提取 userId
     */
    private String extractUserId(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.getUserIdFromToken(token);
            }
        }
        return null;
    }
}
