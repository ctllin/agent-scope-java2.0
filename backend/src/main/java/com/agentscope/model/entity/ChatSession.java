package com.agentscope.model.entity;

import com.agentscope.common.enums.ChatMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 对话会话实体类
 * <p>
 * 存储用户对话会话信息
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_sessions")
public class ChatSession {

    /** 会话ID */
    @Id
    private String id;

    /** 会话标题 */
    private String title;

    /** 用户ID */
    private String userId;

    /** 对话模式 */
    private ChatMode mode;

    /** 使用的AI模型 */
    private String model;

    /** 关联的知识库ID（知识库对话模式） */
    private String knowledgeBaseId;

    /** 创建时间 */
    @CreatedDate
    private LocalDateTime createdAt;

    /** 更新时间 */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
