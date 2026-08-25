package com.agentscope.config;

import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

/**
 * AgentScope配置类
 * <p>
 * 配置HarnessAgent，用于AI对话功能
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentScopeConfig {

    private final GlmConfig glmConfig;

    @Value("${agentscope.workspace:${user.home}/.agentscope/workspace}")
    private String workspacePath;

    @Value("${agentscope.compaction.trigger-messages:30}")
    private int triggerMessages;

    @Value("${agentscope.compaction.keep-messages:10}")
    private int keepMessages;

    /**
     * 配置HarnessAgent
     * <p>
     * HarnessAgent是推荐的入口，集成了工作区、长期记忆、会话持久化等功能
     * 使用OpenAI兼容的GLM模型进行对话
     * </p>
     */
    @Bean
    public HarnessAgent harnessAgent() {
        String apiKey = glmConfig.getApiKey();
        String model = glmConfig.getModel();
        String baseUrl = glmConfig.getBaseUrl();

        // 检查API密钥是否配置
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GLM API密钥未配置，AI对话功能将不可用。请设置GLM_API_KEY环境变量。");
            model = "dummy";
        }

        log.info("初始化HarnessAgent，模型: {}, baseUrl: {}", model, baseUrl != null ? baseUrl : "default");

        try {
            // 使用OpenAIChatModel显式构建模型，确保baseUrl正确传递
            OpenAIChatModel chatModel = OpenAIChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(model)
                    .baseUrl(baseUrl)
                    .stream(true)
                    .build();

            return HarnessAgent.builder()
                    .name("ai-platform-agent")
                    .sysPrompt("你是一个智能助手，可以帮助用户进行对话和知识库查询。请用中文回答问题。")
                    .model(chatModel)
                    .workspace(Paths.get(workspacePath))
                    .compaction(CompactionConfig.builder()
                            .triggerMessages(triggerMessages)
                            .keepMessages(keepMessages)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("初始化HarnessAgent失败: {}", e.getMessage(), e);
            return HarnessAgent.builder()
                    .name("ai-platform-agent")
                    .sysPrompt("你是一个智能助手。")
                    .workspace(Paths.get(workspacePath))
                    .build();
        }
    }
}
