package com.agentscope.events;

/**
 * 业务事件处理器：每种事件类型注册一个实现Bean，
 * DisruptorEventConfig 自动将其接入事件总线并按类型路由。
 */
public interface BizEventHandler {

    /** 处理的事件类型 */
    BizEventType type();

    /**
     * 处理事件（消费线程内执行，耗时操作；异常由总线统一记录）
     */
    void handle(BizEvent event);
}
