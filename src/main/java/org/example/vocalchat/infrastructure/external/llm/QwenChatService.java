package org.example.vocalchat.infrastructure.external.llm;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@ConditionalOnProperty(prefix = "vocal-chat.llm.qwen", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QwenChatService {

    private final ChatModel chatModel;  // 普通对话
    private final ChatModel searchChatModel;  //联网搜索
    private final ChatModel deepThinkingChatModel;  //深度思考
    private final StreamingChatModel streamingChatModel;  //普通流式
    private final StreamingChatModel searchStreamingChatModel;
    private final StreamingChatModel deepThinkingStreamingChatModel;

    public QwenChatService(@Qualifier("qwenChatModel") ChatModel chatModel,
                           @Qualifier("qwenSearchChatModel") ChatModel searchChatModel,
                           @Qualifier("qwenDeepThinkingChatModel") ChatModel deepThinkingChatModel,
                           @Qualifier("qwenStreamingChatModel") StreamingChatModel streamingChatModel,
                           @Qualifier("qwenSearchStreamingChatModel") StreamingChatModel searchStreamingChatModel,
                           @Qualifier("qwenDeepThinkingStreamingChatModel") StreamingChatModel deepThinkingStreamingChatModel) {
        this.chatModel = chatModel;
        this.searchChatModel = searchChatModel;
        this.deepThinkingChatModel = deepThinkingChatModel;
        this.streamingChatModel = streamingChatModel;
        this.searchStreamingChatModel = searchStreamingChatModel;
        this.deepThinkingStreamingChatModel = deepThinkingStreamingChatModel;
    }
    //同步聊天
    public String chat(String userMessage) {
        return chat(List.of(UserMessage.from(userMessage)), QwenChatMode.DEFAULT).aiMessage().text();
    }
    //支持多论对话
    public ChatResponse chat(List<ChatMessage> messages, QwenChatMode mode) {
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .build();
        return chat(request, mode);
    }
    // 完全自定义请求
    public ChatResponse chat(ChatRequest request, QwenChatMode mode) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            return selectChatModel(mode).chat(request);
        } catch (RuntimeException e) {
            throw new BaseException(ErrorEnum.LLM_CALL_FAILED, e);
        }
    }

    public void streamChat(List<ChatMessage> messages, QwenChatMode mode, StreamingChatResponseHandler handler) {
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .build();
        streamChat(request, mode, handler);
    }

    public void streamChat(ChatRequest request, QwenChatMode mode, StreamingChatResponseHandler handler) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        try {
            selectStreamingChatModel(mode).chat(request, handler);
        } catch (RuntimeException e) {
            throw new BaseException(ErrorEnum.LLM_CALL_FAILED, e);
        }
    }
    // 模式选择
    private ChatModel selectChatModel(QwenChatMode mode) {
        return switch (normalize(mode)) {
            case DEFAULT -> chatModel;
            case ONLINE_SEARCH -> searchChatModel;
            case DEEP_THINKING -> deepThinkingChatModel;
        };
    }

    private StreamingChatModel selectStreamingChatModel(QwenChatMode mode) {
        return switch (normalize(mode)) {
            case DEFAULT -> streamingChatModel;
            case ONLINE_SEARCH -> searchStreamingChatModel;
            case DEEP_THINKING -> deepThinkingStreamingChatModel;
        };
    }

    private QwenChatMode normalize(QwenChatMode mode) {
        return mode == null ? QwenChatMode.DEFAULT : mode;
    }
}
