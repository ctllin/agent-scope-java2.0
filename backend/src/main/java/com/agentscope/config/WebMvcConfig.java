package com.agentscope.config;

import com.agentscope.interceptor.RequestIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置
 * <p>
 * 配置拦截器、跨域访问等
 * </p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RequestIdInterceptor requestIdInterceptor;
    @Value("${spring.mvc.static-path-pattern:/files/**}")
    private String pathPatterns;
    @Value("${spring.web.resources.static-locations:file:/data/agent-scope/storage/}")
    private String location;

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestIdInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/quick-login");
    }

    /**
     * 配置跨域访问
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(pathPatterns)
                .addResourceLocations(location)
                // 核心配置：开启目录浏览功能
                .setUseLastModified(false);
    }
}
