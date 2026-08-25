package com.agentscope.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP客户端超时配置
 * 统一设置连接超时和读取超时为5分钟
 */
@Configuration
public class HttpConfig {

    /** 连接超时时间（毫秒）：5分钟 */
    private static final int CONNECT_TIMEOUT = 300_000;
    /** 读取超时时间（毫秒）：5分钟 */
    private static final int READ_TIMEOUT = 300_000;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(factory);
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT))
                .build();
    }
}
