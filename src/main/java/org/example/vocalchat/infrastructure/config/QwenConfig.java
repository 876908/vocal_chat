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

/**
 * Qwen 模型配置。
 *
 * <p>按「对话模式 × 是否流式」定义 6 个 Bean，供 {@code QwenChatService} 通过
 * {@code @Qualifier} 注入。全部消费 {@code vocal-chat.llm.qwen.*} 配置。</p>
 */
@Configuration
@EnableConfigurationProperties(QwenProperties.class)
@ConditionalOnProperty(prefix = "vocal-chat.llm.qwen", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QwenConfig {

    // === 同步模型 ===

    @Bean
    public ChatModel qwenChatModel(QwenProperties properties) {
        return QwenChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .defaultRequestParameters(requestParameters(properties, properties.getModelName(), false, false))
                .build();
    }

    @Bean
    public ChatModel qwenSearchChatModel(QwenProperties properties) {
        return QwenChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .defaultRequestParameters(requestParameters(properties, properties.getSearchModelName(), true, false))
                .build();
    }

    @Bean
    public ChatModel qwenDeepThinkingChatModel(QwenProperties properties) {
        return QwenChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .defaultRequestParameters(requestParameters(properties, properties.getDeepThinkingModelName(), false, true))
                .build();
    }

    // === 流式模型 ===

    @Bean
    public StreamingChatModel qwenStreamingChatModel(QwenProperties properties) {
        return QwenStreamingChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .defaultRequestParameters(requestParameters(properties, properties.getModelName(), false, false))
                .build();
    }

    @Bean
    public StreamingChatModel qwenSearchStreamingChatModel(QwenProperties properties) {
        return QwenStreamingChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .defaultRequestParameters(requestParameters(properties, properties.getSearchModelName(), true, false))
                .build();
    }

    @Bean
    public StreamingChatModel qwenDeepThinkingStreamingChatModel(QwenProperties properties) {
        return QwenStreamingChatModel.builder()
                .apiKey(properties.getApiKey())
                .baseUrl(properties.getBaseUrl())
                .defaultRequestParameters(requestParameters(properties, properties.getDeepThinkingModelName(), false, true))
                .build();
    }

    /**
     * 组装各模型共用的请求默认参数，再叠加模式相关的联网搜索 / 深度思考开关。
     */
    private QwenChatRequestParameters requestParameters(QwenProperties properties,
                                                        String modelName,
                                                        boolean enableSearch,
                                                        boolean enableThinking) {
        QwenChatRequestParameters.Builder builder = QwenChatRequestParameters.builder()
                .modelName(modelName)
                .topP(properties.getTopP())
                .topK(properties.getTopK())
                .maxOutputTokens(properties.getMaxTokens())
                .supportIncrementalOutput(properties.getSupportIncrementalOutput())
                .enableSanitizeMessages(properties.getEnableSanitizeMessages());

        if (properties.getTemperature() != null) {
            builder.temperature(properties.getTemperature().doubleValue());
        }
        if (enableSearch) {
            builder.enableSearch(true);
        }
        if (enableThinking) {
            builder.enableThinking(true);
            builder.thinkingBudget(properties.getThinkingBudget());
        }

        return builder.build();
    }
}
