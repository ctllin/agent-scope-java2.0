package com.agentscope.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB审计配置
 * <p>
 * 启用@CreatedDate/@LastModifiedDate自动填充，
 * 实体创建/更新时间才能落库，列表按创建时间排序依赖此字段。
 */
@Configuration
@EnableMongoAuditing
public class MongoAuditingConfig {
}
