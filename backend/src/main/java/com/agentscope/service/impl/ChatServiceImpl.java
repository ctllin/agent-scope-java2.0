package com.agentscope.service.impl;

import com.agentscope.common.RequestContext;
import com.agentscope.common.ResultCode;
import com.agentscope.common.enums.ChatMode;
import com.agentscope.common.exception.BusinessException;
import com.agentscope.model.dto.CreateChatSessionRequest;
import com.agentscope.model.dto.SendMessageRequest;
import com.agentscope.model.entity.ChatMessage;
import com.agentscope.model.entity.ChatSession;
import com.agentscope.model.vo.ChatMessageVO;
import com.agentscope.model.vo.ChatSessionVO;
import com.agentscope.model.vo.SearchResultVO;
import com.agentscope.model.vo.SendMessageResponse;
import com.agentscope.repository.ChatMessageRepository;
import com.agentscope.repository.ChatSessionRepository;
import com.agentscope.service.ChatService;
import com.agentscope.service.KnowledgeBaseService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话服务实现类
 * 使用AgentScope HarnessAgent处理AI对话
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final HarnessAgent harnessAgent;
    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    public ChatSessionVO createSession(CreateChatSessionRequest request) {
        String userId = RequestContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        ChatSession session = ChatSession.builder()
                .title(request.getTitle() != null ? request.getTitle() : "新对话")
                .userId(userId)
                .mode(request.getMode() != null ? request.getMode() : ChatMode.NORMAL)
                .model(request.getModel())
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .build();

        session = chatSessionRepository.save(session);
        return convertToVO(session);
    }

    @Override
    public List<ChatSessionVO> listSessions() {
        String userId = RequestContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ChatSessionVO getSession(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "会话不存在"));
        return convertToVO(session);
    }

    @Override
    public void deleteSession(String sessionId) {
        if (!chatSessionRepository.existsById(sessionId)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "会话不存在");
        }

        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId);
        chatMessageRepository.deleteAll(messages);
        chatSessionRepository.deleteById(sessionId);
    }

    /**
     * 发送消息并获取AI回复
     * 返回一对消息（用户消息+AI回复）+ 耗时
     */
    @Override
    public SendMessageResponse sendMessage(SendMessageRequest request) {
        ChatSession session = chatSessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "会话不存在"));

        String userId = RequestContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 保存用户消息
        ChatMessage userMessage = ChatMessage.builder()
                .sessionId(request.getSessionId())
                .role("user")
                .content(request.getContent())
                .build();
        userMessage = chatMessageRepository.save(userMessage);

        // 记录开始时间
        LocalDateTime startTime = LocalDateTime.now();

        // 使用AgentScope HarnessAgent生成回复
        String aiReply = generateAiReplyWithAgentScope(session, request.getContent(), userId);

        // 计算耗时
        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = ChronoUnit.MILLIS.between(startTime, endTime);

        // 保存AI回复
        ChatMessage aiMessage = ChatMessage.builder()
                .sessionId(request.getSessionId())
                .role("assistant")
                .content(aiReply)
                .tokens(aiReply.length())
                .duration(durationMs)
                .build();
        aiMessage = chatMessageRepository.save(aiMessage);

        // 返回一对消息
        return SendMessageResponse.builder()
                .userMessage(convertToMessageVO(userMessage))
                .aiMessage(convertToMessageVO(aiMessage))
                .build();
    }

    @Override
    public List<ChatMessageVO> getSessionMessages(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAt(sessionId).stream()
                .map(this::convertToMessageVO)
                .collect(Collectors.toList());
    }

    private String generateAiReplyWithAgentScope(ChatSession session, String userMessage, String userId) {
        try {
            String enrichedMessage = userMessage;
            if (session.getMode() == ChatMode.KNOWLEDGE_BASE && session.getKnowledgeBaseId() != null) {
                enrichedMessage = buildKnowledgeBasePrompt(session.getKnowledgeBaseId(), userMessage);
            }

            RuntimeContext runtimeContext = RuntimeContext.builder()
                    .userId(userId)
                    .sessionId(session.getId())
                    .build();

            var response = harnessAgent.call(
                    new UserMessage(enrichedMessage),
                    runtimeContext
            ).block();

            if (response != null) {
                return response.getTextContent();
            }

            return "抱歉，无法生成回复。";
        } catch (Exception e) {
            log.error("AI对话处理失败", e);
            return "处理请求时出现错误，请稍后重试。";
        }
    }

    private String buildKnowledgeBasePrompt(String knowledgeBaseId, String userMessage) {
        try {
            List<SearchResultVO> searchResults = knowledgeBaseService.searchVectors(
                    knowledgeBaseId, userMessage, 5);

            if (searchResults.isEmpty()) {
                return userMessage;
            }

            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("以下是从知识库中检索到的相关信息，请基于这些信息回答用户的问题。");
            contextBuilder.append("如果检索到的信息与问题无关，请忽略这些信息并用自己的知识回答。\n\n");
            contextBuilder.append("【检索到的相关信息】\n");

            for (int i = 0; i < searchResults.size(); i++) {
                SearchResultVO result = searchResults.get(i);
                contextBuilder.append(String.format("%d. %s（来源：%s，相关度：%.2f）\n",
                        i + 1, result.getContent(), result.getDocumentName(), result.getScore()));
            }

            contextBuilder.append("\n【用户问题】\n").append(userMessage);
            return contextBuilder.toString();
        } catch (Exception e) {
            log.error("知识库检索失败，使用原始问题", e);
            return userMessage;
        }
    }

    private ChatSessionVO convertToVO(ChatSession session) {
        return ChatSessionVO.builder()
                .id(session.getId())
                .title(session.getTitle())
                .mode(session.getMode())
                .model(session.getModel())
                .knowledgeBaseId(session.getKnowledgeBaseId())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private ChatMessageVO convertToMessageVO(ChatMessage message) {
        return ChatMessageVO.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .tokens(message.getTokens())
                .duration(message.getDuration())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
