package com.agentscope.repository;

import com.agentscope.model.entity.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 对话会话数据访问层
 */
@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {

    /**
     * 根据用户ID查找会话列表
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 根据用户ID和会话模式查找会话列表
     * @param userId 用户ID
     * @param mode 会话模式
     * @return 会话列表
     */
    List<ChatSession> findByUserIdAndModeOrderByCreatedAtDesc(String userId, String mode);

    List<ChatSession> findByKnowledgeBaseId(String knowledgeBaseId);
}
