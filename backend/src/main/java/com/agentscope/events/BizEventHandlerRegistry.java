package com.agentscope.events;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件处理器注册表。
 * <p>
 * 处理器实现Bean在初始化时自行调用 {@link #register} 注册，
 * 事件总线仅依赖本注册表、不感知任何具体处理器，
 * 从而保证依赖方向单一：业务服务 → 发布器 → 总线 → 注册表 ← 处理器 ← 业务服务（无环）。
 */
@Component
public class BizEventHandlerRegistry {

    private final Map<BizEventType, List<BizEventHandler>> handlers = new EnumMap<>(BizEventType.class);

    public synchronized void register(BizEventHandler handler) {
        handlers.computeIfAbsent(handler.type(), k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /** 获取某类型的处理器列表（可能为空） */
    public List<BizEventHandler> get(BizEventType type) {
        return handlers.getOrDefault(type, Collections.emptyList());
    }
}
