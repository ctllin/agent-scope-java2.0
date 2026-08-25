package com.agentscope.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 对话消息实体类
 * 存储对话消息内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    /** 所属会话ID */
    private String sessionId;

    /** 消息角色：user-用户，assistant-助手 */
    private String role;

    /** 消息内容 */
    private String content;

    /** Token使用量 */
    private Integer tokens;

    /** AI回复耗时（毫秒） */
    private Long duration;

    /** 创建时间 */
    @CreatedDate
    private LocalDateTime createdAt;
}
