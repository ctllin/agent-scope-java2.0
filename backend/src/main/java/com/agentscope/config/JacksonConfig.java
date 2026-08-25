package com.agentscope.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson配置
 * <p>
 * 配置JSON序列化/反序列化
 * </p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());

        // 不将日期序列化为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 反序列化时忽略未知字段（与Spring Boot默认行为一致）
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }
}
