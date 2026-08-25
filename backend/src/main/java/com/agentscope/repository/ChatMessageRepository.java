package com.agentscope.repository;

import com.agentscope.model.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 对话消息数据访问层
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 根据会话ID查找消息列表
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAt(String sessionId);

    /**
     * 根据会话ID查找最近的消息
     * @param sessionId 会话ID
     * @param limit 数量限制
     * @return 消息列表
     */
    List<ChatMessage> findTop10BySessionIdOrderByCreatedAtDesc(String sessionId);
}
