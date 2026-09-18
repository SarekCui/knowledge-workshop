package com.knowledge.agent.model.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AgentModelProperties.class)
public class AgentModelConfiguration {

    @Bean
    @ConditionalOnProperty(name = "knowledge.agent.model.enabled", havingValue = "true")
    ChatModel deepSeekChatModel(AgentModelProperties properties) {
        validateApiKey(properties);
        return OpenAiChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getName())
                .maxCompletionTokens(properties.getMaxOutputTokens())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .maxRetries(0)
                .returnThinking(false)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "knowledge.agent.model.enabled", havingValue = "true")
    StreamingChatModel deepSeekStreamingChatModel(AgentModelProperties properties) {
        validateApiKey(properties);
        return OpenAiStreamingChatModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getName())
                .maxCompletionTokens(properties.getMaxOutputTokens())
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .returnThinking(false)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private void validateApiKey(AgentModelProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("启用小智模型前必须设置 DEEPSEEK_API_KEY");
        }
    }
}
