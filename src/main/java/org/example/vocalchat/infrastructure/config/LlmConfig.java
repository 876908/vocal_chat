package org.example.vocalchat.infrastructure.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenChatRequestParameters;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.example.vocalchat.infrastructure.config.properties.QwenProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(QwenProperties.class)
@ConditionalOnProperty(prefix = "vocal-chat.llm.qwen", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LlmConfig {

    @Bean("qwenChatModel")
    @Primary
    public ChatModel qwenChatModel(QwenProperties properties) {
        return qwenChatModel(properties, properties.getModelName(), false, false);
    }

    @Bean("qwenSearchChatModel")
    public ChatModel qwenSearchChatModel(QwenProperties properties) {
        return qwenChatModel(properties, properties.getSearchModelName(), true, false);
    }

    @Bean("qwenDeepThinkingChatModel")
    public ChatModel qwenDeepThinkingChatModel(QwenProperties properties) {
        return qwenChatModel(properties, properties.getDeepThinkingModelName(), false, true);
    }

    @Bean("qwenStreamingChatModel")
    @Primary
    public StreamingChatModel qwenStreamingChatModel(QwenProperties properties) {
        return qwenStreamingChatModel(properties, properties.getModelName(), false, false);
    }

    @Bean("qwenSearchStreamingChatModel")
    public StreamingChatModel qwenSearchStreamingChatModel(QwenProperties properties) {
        return qwenStreamingChatModel(properties, properties.getSearchModelName(), true, false);
    }

    @Bean("qwenDeepThinkingStreamingChatModel")
    public StreamingChatModel qwenDeepThinkingStreamingChatModel(QwenProperties properties) {
        return qwenStreamingChatModel(properties, properties.getDeepThinkingModelName(), false, true);
    }

    private ChatModel qwenChatModel(QwenProperties properties, String modelName,
                                   boolean enableSearch, boolean enableThinking) {
        QwenChatModel.QwenChatModelBuilder builder = QwenChatModel.builder()
                .modelName(modelName)
                .defaultRequestParameters(requestParameters(properties, modelName, enableSearch, enableThinking));

        applySharedModelOptions(builder, properties);
        return builder.build();
    }

    private StreamingChatModel qwenStreamingChatModel(QwenProperties properties, String modelName,
                                                     boolean enableSearch, boolean enableThinking) {
        QwenStreamingChatModel.QwenStreamingChatModelBuilder builder = QwenStreamingChatModel.builder()
                .modelName(modelName)
                .defaultRequestParameters(requestParameters(properties, modelName, enableSearch, enableThinking));

        applySharedModelOptions(builder, properties);
        return builder.build();
    }

    private QwenChatRequestParameters requestParameters(QwenProperties properties, String modelName,
                                                       boolean enableSearch, boolean enableThinking) {
        QwenChatRequestParameters.Builder builder = QwenChatRequestParameters.builder()
                .modelName(modelName)
                .topP(properties.getTopP())
                .maxOutputTokens(properties.getMaxTokens())
                .enableSearch(enableSearch)
                .enableThinking(enableThinking)
                .supportIncrementalOutput(properties.getSupportIncrementalOutput())
                .enableSanitizeMessages(properties.getEnableSanitizeMessages());

        if (properties.getTemperature() != null) {
            builder.temperature(properties.getTemperature().doubleValue());
        }
        if (properties.getTopK() != null) {
            builder.topK(properties.getTopK());
        }
        if (enableThinking && properties.getThinkingBudget() != null) {
            builder.thinkingBudget(properties.getThinkingBudget());
        }

        return builder.build();
    }

    private void applySharedModelOptions(QwenChatModel.QwenChatModelBuilder builder, QwenProperties properties) {
        if (StringUtils.hasText(properties.getApiKey())) {
            builder.apiKey(properties.getApiKey());
        }
        if (StringUtils.hasText(properties.getBaseUrl())) {
            builder.baseUrl(properties.getBaseUrl());
        }
        if (properties.getTemperature() != null) {
            builder.temperature(properties.getTemperature());
        }
        if (properties.getTopP() != null) {
            builder.topP(properties.getTopP());
        }
        if (properties.getTopK() != null) {
            builder.topK(properties.getTopK());
        }
        if (properties.getMaxTokens() != null) {
            builder.maxTokens(properties.getMaxTokens());
        }
    }

    private void applySharedModelOptions(QwenStreamingChatModel.QwenStreamingChatModelBuilder builder,
                                         QwenProperties properties) {
        if (StringUtils.hasText(properties.getApiKey())) {
            builder.apiKey(properties.getApiKey());
        }
        if (StringUtils.hasText(properties.getBaseUrl())) {
            builder.baseUrl(properties.getBaseUrl());
        }
        if (properties.getTemperature() != null) {
            builder.temperature(properties.getTemperature());
        }
        if (properties.getTopP() != null) {
            builder.topP(properties.getTopP());
        }
        if (properties.getTopK() != null) {
            builder.topK(properties.getTopK());
        }
        if (properties.getMaxTokens() != null) {
            builder.maxTokens(properties.getMaxTokens());
        }
    }
}
