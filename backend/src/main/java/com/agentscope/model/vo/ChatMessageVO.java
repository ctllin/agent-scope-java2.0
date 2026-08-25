package com.agentscope.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话消息视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    /** 消息ID */
    private String id;

    /** 消息角色（user/assistant） */
    private String role;

    /** 消息内容 */
    private String content;

    /** Token使用量 */
    private Integer tokens;

    /** AI回复耗时（毫秒） */
    private Long duration;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
