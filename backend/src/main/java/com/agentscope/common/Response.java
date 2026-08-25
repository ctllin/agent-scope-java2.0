package com.agentscope.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体
 * <p>
 * 所有API响应必须使用此格式进行封装。
 * 包含请求ID、时间戳、状态码、消息和业务数据。
 * </p>
 *
 * @param <T> 业务数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> {

    /** 请求ID，用于日志追踪和问题定位 */
    private String requestId;
    
    /** 响应时间戳（毫秒） */
    private Long timestamp;
    
    /** 业务状态码，200表示成功 */
    private int code;
    
    /** 状态消息 */
    private String message;
    
    /** 业务返回数据 */
    private T data;

    /**
     * 构建成功响应（带数据）
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 成功响应对象
     */
    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .requestId(RequestContext.getRequestId())
                .timestamp(System.currentTimeMillis())
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    /**
     * 构建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功响应对象
     */
    public static <T> Response<T> success() {
        return Response.<T>builder()
                .requestId(RequestContext.getRequestId())
                .timestamp(System.currentTimeMillis())
                .code(ResultCode.SUCCESS.getCode())
                .message(ResultCode.SUCCESS.getMessage())
                .build();
    }

    /**
     * 构建错误响应（使用ResultCode）
     *
     * @param resultCode 错误码
     * @param <T>        数据类型
     * @return 错误响应对象
     */
    public static <T> Response<T> error(ResultCode resultCode) {
        return Response.<T>builder()
                .requestId(RequestContext.getRequestId())
                .timestamp(System.currentTimeMillis())
                .code(resultCode.getCode())
                .message(resultCode.getMessage())
                .build();
    }

    /**
     * 构建错误响应（自定义错误码和消息）
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 错误响应对象
     */
    public static <T> Response<T> error(int code, String message) {
        return Response.<T>builder()
                .requestId(RequestContext.getRequestId())
                .timestamp(System.currentTimeMillis())
                .code(code)
                .message(message)
                .build();
    }
}
