package com.agentscope.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一请求体
 * <p>
 * 所有API请求必须使用此格式进行封装。
 * 包含请求ID、用户ID、时间戳和业务数据。
 * </p>
 *
 * @param <T> 业务数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request<T> {

    /** 请求唯一标识，用于日志追踪 */
    private String requestId;
    
    /** 发起请求的用户ID */
    private String userId;
    
    /** 请求时间戳（毫秒） */
    private Long timestamp;
    
    /** 业务数据 */
    private T data;

    /**
     * 快速构建请求对象
     *
     * @param data 业务数据
     * @param <T>  数据类型
     * @return 封装后的请求对象
     */
    public static <T> Request<T> of(T data) {
        return Request.<T>builder()
                .requestId(RequestContext.getRequestId())
                .userId(RequestContext.getUserId())
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }
}
