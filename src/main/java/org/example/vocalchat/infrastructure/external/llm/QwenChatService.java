package org.example.vocalchat.infrastructure.external.llm;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class QwenChatService {

    private static final Logger log = LoggerFactory.getLogger(QwenChatService.class);

    private final StreamingChatModel streamingChatModel;

    public QwenChatService(StreamingChatModel streamingChatModel) {
        this.streamingChatModel = streamingChatModel;
    }

    public CompletableFuture<String> streamChat(String systemPrompt, List<ChatMessage> history,
                                                 String userMessage,
                                                 Consumer<String> onToken,
                                                 Consumer<String> onThinking,
                                                 Runnable onComplete,
                                                 Consumer<Throwable> onError) {

        var messages = new java.util.ArrayList<ChatMessage>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(SystemMessage.from(systemPrompt));
        }
        messages.addAll(history);
        messages.add(UserMessage.from(userMessage));

        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder fullResponse = new StringBuilder();

        streamingChatModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                fullResponse.append(partialResponse);
                onToken.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                String text = fullResponse.toString();
                onComplete.run();
                future.complete(text);
            }

            @Override
            public void onError(Throwable error) {
                log.error("LLM stream error: {}", error.getMessage(), error);
                onError.accept(error);
                future.completeExceptionally(error);
            }
        });

        return future;
    }

    public CompletableFuture<String> streamChat(String systemPrompt,
                                                 String userMessage,
                                                 Consumer<String> onToken,
                                                 Consumer<String> onThinking,
                                                 Runnable onComplete,
                                                 Consumer<Throwable> onError) {
        return streamChat(systemPrompt, List.of(), userMessage, onToken, onThinking, onComplete, onError);
    }
}
