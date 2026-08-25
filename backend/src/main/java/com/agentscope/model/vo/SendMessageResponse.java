package com.agentscope.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送消息响应
 * 返回用户消息和AI回复的一对消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageResponse {

    /** 用户消息 */
    private ChatMessageVO userMessage;

    /** AI回复 */
    private ChatMessageVO aiMessage;
}
