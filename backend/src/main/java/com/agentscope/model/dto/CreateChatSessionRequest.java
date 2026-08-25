package com.agentscope.model.dto;

import com.agentscope.common.enums.ChatMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建对话会话请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatSessionRequest {

    /** 会话标题 */
    private String title;

    /** 对话模式 */
    private ChatMode mode;

    /** 使用的AI模型 */
    private String model;

    /** 关联的知识库ID（知识库对话模式） */
    private String knowledgeBaseId;
}
