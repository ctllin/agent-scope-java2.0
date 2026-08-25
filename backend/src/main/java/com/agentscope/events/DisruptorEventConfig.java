package com.agentscope.events;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Disruptor事件总线配置（默认实现，与具体业务完全解耦）。
 * <p>
 * 依赖方向：业务服务 → EventPublisher → Disruptor → 注册表 ← 各处理器自注册。
 * 总线只做统一分发：运行时按事件类型查注册表调用，异常仅记录日志，
 * 失败状态由任务自身标记，支持前端手动重试。
 * <p>
 * 预留切换：app.events.type=kafka 时可提供同接口的Kafka实现替换本配置。
 */
@Slf4j
@Configuration
public class DisruptorEventConfig {

    @Value("${app.events.ring-buffer-size:1024}")
    private int ringBufferSize;

    @Bean(destroyMethod = "shutdown")
    public Disruptor<BizEvent> bizEventDisruptor(BizEventHandlerRegistry registry) {
        Disruptor<BizEvent> disruptor = new Disruptor<>(
                BizEvent::new,
                Math.max(ringBufferSize, 64),
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI,
                new BlockingWaitStrategy());

        // 单一分发消费者：运行时按类型路由到注册表中的处理器
        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            if (event.getType() == null) return;
            // 恢复发布线程的traceId到消费线程MDC；无则生成新ID
            if (event.getTraceId() != null && !event.getTraceId().isBlank()) {
                org.slf4j.MDC.put("traceId", event.getTraceId());
            } else {
                org.slf4j.MDC.put("traceId", com.agentscope.common.RequestContext.generateRequestId());
            }
            long start = System.currentTimeMillis();
            for (BizEventHandler handler : registry.get(event.getType())) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    log.error("事件处理失败: type={}, bizId={}, handler={}",
                            event.getType(), event.getBizId(), handler.getClass().getSimpleName(), e);
                }
            }
            log.debug("事件消费完成: type={}, bizId={}, 耗时={}ms",
                    event.getType(), event.getBizId(), System.currentTimeMillis() - start);
            org.slf4j.MDC.clear();
        });
        disruptor.start();
        log.info("Disruptor事件总线已启动: bufferSize={}", Math.max(ringBufferSize, 64));
        return disruptor;
    }

    @Bean
    public EventPublisher eventPublisher(Disruptor<BizEvent> disruptor) {
        return (type, bizId, params) -> disruptor
                .getRingBuffer()
                .publishEvent((event, sequence) -> {
                    event.setType(type);
                    event.setBizId(bizId);
                    event.setParams(params == null ? Map.of() : params);
                    event.setPublishAt(System.currentTimeMillis());
                    // 捕获发布线程的traceId，消费线程恢复后日志可跨线程串联
                    event.setTraceId(org.slf4j.MDC.get("traceId"));
                });
    }
}
