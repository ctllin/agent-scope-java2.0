package com.agentscope.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送消息请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /** 会话ID */
    private String sessionId;

    /** 消息内容 */
    private String content;
}
