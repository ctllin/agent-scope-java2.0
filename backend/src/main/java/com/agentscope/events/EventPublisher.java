package com.agentscope.events;

import java.util.Map;

/**
 * 事件发布器抽象（默认Disruptor实现，预留Kafka切换）
 */
public interface EventPublisher {

    /** 发布业务事件，立即返回 */
    void publish(BizEventType type, String bizId, Map<String, String> params);
}
