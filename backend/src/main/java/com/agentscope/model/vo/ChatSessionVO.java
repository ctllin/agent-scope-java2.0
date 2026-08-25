package com.agentscope.model.vo;

import com.agentscope.common.enums.ChatMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话会话视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO {

    /** 会话ID */
    private String id;

    /** 会话标题 */
    private String title;

    /** 对话模式 */
    private ChatMode mode;

    /** 使用的AI模型 */
    private String model;

    /** 关联的知识库ID */
    private String knowledgeBaseId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
