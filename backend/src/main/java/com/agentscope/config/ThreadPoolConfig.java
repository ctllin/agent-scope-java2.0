package com.agentscope.config;

import com.agentscope.common.RequestContext;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * <p>
 * 自定义线程池，支持MDC上下文传递，确保异步任务能够继承主线程的日志上下文。
 * </p>
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 业务线程池
     * <p>
     * 用于处理一般业务逻辑的异步任务
     * </p>
     */
    @Bean("businessExecutor")
    public Executor businessExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("business-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * AI处理线程池
     * <p>
     * 用于处理AI对话等耗时操作
     * </p>
     */
    @Bean("aiExecutor")
    public Executor aiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("ai-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 定时任务调度器。
     * <p>
     * 供@Scheduled使用（Spring自动发现唯一TaskScheduler Bean）。
     * 与线程池装饰器不同：定时任务没有"发起请求的父线程"，无法继承traceId，
     * 因此每次执行<b>生成全新的traceId</b>，保证单次调度内所有日志可串联、
     * 多次调度之间相互隔离。
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sched-");
        scheduler.setTaskDecorator(runnable -> () -> {
            // 每次调度执行生成独立traceId
            MDC.put("traceId", RequestContext.generateRequestId());
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        });
        scheduler.initialize();
        return scheduler;
    }

    /**
     * MDC任务装饰器
     * <p>
     * 确保异步任务能够继承主线程的MDC上下文，实现日志全链路追踪
     * </p>
     */
    private static class MdcTaskDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {
            // 捕获当前线程的MDC上下文
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    // 在异步线程中设置MDC上下文
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    // 清理MDC上下文，防止内存泄漏
                    MDC.clear();
                }
            };
        }
    }
}
