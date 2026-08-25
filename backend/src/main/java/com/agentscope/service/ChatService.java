package com.agentscope.service;

import com.agentscope.model.dto.CreateChatSessionRequest;
import com.agentscope.model.dto.SendMessageRequest;
import com.agentscope.model.vo.ChatMessageVO;
import com.agentscope.model.vo.ChatSessionVO;
import com.agentscope.model.vo.SendMessageResponse;

import java.util.List;

/**
 * 对话服务接口
 */
public interface ChatService {

    /** 创建对话会话 */
    ChatSessionVO createSession(CreateChatSessionRequest request);

    /** 获取用户的所有会话 */
    List<ChatSessionVO> listSessions();

    /** 获取会话详情 */
    ChatSessionVO getSession(String sessionId);

    /** 删除会话 */
    void deleteSession(String sessionId);

    /** 发送消息并获取AI回复（返回一对消息+耗时） */
    SendMessageResponse sendMessage(SendMessageRequest request);

    /** 获取会话历史消息 */
    List<ChatMessageVO> getSessionMessages(String sessionId);
}
