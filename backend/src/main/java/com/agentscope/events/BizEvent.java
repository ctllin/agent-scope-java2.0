package com.agentscope.events;

import lombok.Data;

import java.util.Map;

/**
 * 业务事件（Disruptor槽位复用对象，消费者内不得缓存引用）
 */
@Data
public class BizEvent {

    private BizEventType type;

    /** 业务ID：documentId / recordId */
    private String bizId;

    /** 扩展参数（如 lang） */
    private Map<String, String> params;

    /** 发布时间戳 */
    private long publishAt;

    /**
     * 发布线程的链路追踪ID（发布时从MDC捕获，消费线程恢复，实现异步日志串联）
     */
    private String traceId;
}
