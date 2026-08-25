package com.agentscope.controller;

import com.agentscope.common.Response;
import com.agentscope.model.dto.CreateChatSessionRequest;
import com.agentscope.model.dto.SendMessageRequest;
import com.agentscope.model.vo.ChatMessageVO;
import com.agentscope.model.vo.ChatSessionVO;
import com.agentscope.model.vo.SendMessageResponse;
import com.agentscope.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话管理控制器
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public Response<ChatSessionVO> createSession(@Valid @RequestBody CreateChatSessionRequest request) {
        return Response.success(chatService.createSession(request));
    }

    @GetMapping("/sessions")
    public Response<List<ChatSessionVO>> listSessions() {
        return Response.success(chatService.listSessions());
    }

    @GetMapping("/sessions/{id}")
    public Response<ChatSessionVO> getSession(@PathVariable String id) {
        return Response.success(chatService.getSession(id));
    }

    @DeleteMapping("/sessions/{id}")
    public Response<Void> deleteSession(@PathVariable String id) {
        chatService.deleteSession(id);
        return Response.success();
    }

    /**
     * 发送消息，返回一对消息（用户消息+AI回复）
     */
    @PostMapping("/messages")
    public Response<SendMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return Response.success(chatService.sendMessage(request));
    }

    @GetMapping("/sessions/{id}/messages")
    public Response<List<ChatMessageVO>> getSessionMessages(@PathVariable String id) {
        return Response.success(chatService.getSessionMessages(id));
    }
}
