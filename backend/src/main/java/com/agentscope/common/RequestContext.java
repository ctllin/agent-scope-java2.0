package com.agentscope.common;

import java.util.UUID;

/**
 * 请求上下文
 * <p>
 * 用于在请求处理过程中传递请求级别的数据，如请求ID、用户ID等。
 * 使用ThreadLocal存储，确保线程安全。
 * </p>
 */
public class RequestContext {

    /** 请求ID的ThreadLocal存储 */
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    
    /** 用户ID的ThreadLocal存储 */
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    /**
     * 生成唯一的请求ID
     * @return UUID格式的请求ID（去除横线）
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 获取当前请求的请求ID
     * @return 请求ID，如果不存在则自动生成
     */
    public static String getRequestId() {
        String requestId = REQUEST_ID.get();
        if (requestId == null) {
            requestId = generateRequestId();
            REQUEST_ID.set(requestId);
        }
        return requestId;
    }

    /**
     * 设置请求ID
     * @param requestId 请求ID
     */
    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    /**
     * 获取当前用户ID
     * @return 用户ID
     */
    public static String getUserId() {
        return USER_ID.get();
    }

    /**
     * 设置用户ID
     * @param userId 用户ID
     */
    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    /**
     * 清理当前线程的所有上下文数据
     * <p>
     * 必须在请求完成后调用，防止内存泄漏
     * </p>
     */
    public static void clear() {
        REQUEST_ID.remove();
        USER_ID.remove();
    }
}
