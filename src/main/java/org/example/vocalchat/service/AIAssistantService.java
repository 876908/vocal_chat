package org.example.vocalchat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.dto.request.CreateAssistantRequest;
import org.example.vocalchat.dto.request.ModifyAssistantConfigRequest;
import org.example.vocalchat.dto.request.StreamRequest;
import org.example.vocalchat.dto.response.AIAssistantVO;
import org.example.vocalchat.entity.AIAssistant;
import org.example.vocalchat.entity.Dialogue;
import org.example.vocalchat.infrastructure.external.llm.QwenChatMode;
import org.example.vocalchat.infrastructure.external.llm.QwenChatService;
import org.example.vocalchat.mapper.AIAssistantMapper;
import org.example.vocalchat.mapper.DialogueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AIAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AIAssistantService.class);
    private static final long SSE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long HEARTBEAT_INTERVAL_MS = 15000;

    private final AIAssistantMapper assistantMapper;
    private final DialogueMapper dialogueMapper;
    private final QwenChatService qwenChatService;
    private final ObjectMapper objectMapper;

    public AIAssistantService(AIAssistantMapper assistantMapper,
                              DialogueMapper dialogueMapper,
                              QwenChatService qwenChatService,
                              ObjectMapper objectMapper) {
        this.assistantMapper = assistantMapper;
        this.dialogueMapper = dialogueMapper;
        this.qwenChatService = qwenChatService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String createAssistant(CreateAssistantRequest request) {
        String userId = UserContext.requireUserId();

        AIAssistant assistant = new AIAssistant();
        assistant.setUserId(userId);
        assistant.setName(request.getName());
        assistant.setDescription(request.getDescription());
        assistant.setAssistantCharacter(request.getCharacter());
        assistant.setKnowledgeBaseId(request.getKnowledgeBaseId());
        assistant.setCreatedAt(LocalDateTime.now());
        assistant.setUpdatedAt(LocalDateTime.now());
        assistantMapper.insert(assistant);

        Dialogue dialogue = new Dialogue();
        dialogue.setAiAssistantId(assistant.getId());
        dialogue.setContexts(toJson(List.of()));
        dialogue.setCreatedAt(LocalDateTime.now());
        dialogue.setUpdatedAt(LocalDateTime.now());
        dialogueMapper.insert(dialogue);

        log.info("User {} created assistant: id={}, name={}", userId, assistant.getId(), assistant.getName());
        return assistant.getId();
    }

    public List<AIAssistantVO> listAssistants() {
        String userId = UserContext.requireUserId();
        List<AIAssistant> assistants = assistantMapper.selectList(
                new LambdaQueryWrapper<AIAssistant>()
                        .eq(AIAssistant::getUserId, userId)
                        .orderByDesc(AIAssistant::getUpdatedAt));
        return assistants.stream().map(a -> {
            AIAssistantVO vo = new AIAssistantVO();
            vo.setId(a.getId());
            vo.setName(a.getName());
            vo.setDescription(a.getDescription());
            vo.setCharacter(a.getAssistantCharacter());
            vo.setKnowledgeBaseId(a.getKnowledgeBaseId());
            return vo;
        }).toList();
    }

    @Transactional
    public void modifyConfig(ModifyAssistantConfigRequest request) {
        String userId = UserContext.requireUserId();
        AIAssistant assistant = getAndValidateOwnership(request.getAiAssistantId(), userId);
        if (request.getName() != null) assistant.setName(request.getName());
        if (request.getDescription() != null) assistant.setDescription(request.getDescription());
        if (request.getCharacter() != null) assistant.setAssistantCharacter(request.getCharacter());
        if (request.getKnowledgeBaseId() != null) assistant.setKnowledgeBaseId(request.getKnowledgeBaseId());
        assistant.setUpdatedAt(LocalDateTime.now());
        assistantMapper.updateById(assistant);
        log.info("User {} modified assistant: id={}", userId, assistant.getId());
    }

    @Transactional
    public void deleteAssistant(String assistantId) {
        String userId = UserContext.requireUserId();
        getAndValidateOwnership(assistantId, userId);
        dialogueMapper.delete(new LambdaQueryWrapper<Dialogue>()
                .eq(Dialogue::getAiAssistantId, assistantId));
        assistantMapper.deleteById(assistantId);
        log.info("User {} deleted assistant: id={}", userId, assistantId);
    }

    public List<List<String>> getConversationLog(String assistantId) {
        String userId = UserContext.requireUserId();
        getAndValidateOwnership(assistantId, userId);
        Dialogue dialogue = findDialogue(assistantId);
        return parseContexts(dialogue.getContexts());
    }

    @Transactional
    public void resetConversation(String assistantId) {
        String userId = UserContext.requireUserId();
        getAndValidateOwnership(assistantId, userId);
        //这里的 findDialogue(assistantId) 确实是在调用对象方法，只是对象被省略了。
        //它完整等价于Dialogue dialogue = this.findDialogue(assistantId);
        //Java 在实例方法内部调用同类的其他实例方法时，会隐式补上this，所以不需要写
        Dialogue dialogue = findDialogue(assistantId);
        dialogue.setContexts(toJson(List.of()));
        dialogue.setUpdatedAt(LocalDateTime.now());
        dialogueMapper.updateById(dialogue);
        log.info("User {} reset conversation: assistantId={}", userId, assistantId);
    }

    public SseEmitter streamGenerateReply(StreamRequest request) {
        String userId = UserContext.requireUserId();
        AIAssistant assistant = getAndValidateOwnership(request.getAiAssistantId(), userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        Dialogue dialogue = findDialogue(assistant.getId());

        emitter.onCompletion(() -> log.debug("SSE completed: assistantId={}", assistant.getId()));
        emitter.onTimeout(() -> log.debug("SSE timed out: assistantId={}", assistant.getId()));

        try {
            emitter.send(SseEmitter.event().name("started").data("invoked"));
        } catch (IOException e) {
            log.error("SSE send started failed: {}", e.getMessage());
        }

        Thread heartbeatThread = startHeartbeat(emitter);

        String systemPrompt = buildSystemPrompt(assistant);
        List<List<String>> historyMessages = parseContexts(dialogue.getContexts());

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(SystemMessage.from(systemPrompt));
        for (List<String> entry : historyMessages) {
            if (entry.size() < 2) continue;
            String role = entry.get(0);
            String content = entry.get(1);
            if ("USER".equalsIgnoreCase(role)) {
                chatMessages.add(UserMessage.from(content));
            } else if ("ASSISTANT".equalsIgnoreCase(role)) {
                chatMessages.add(AiMessage.from(content));
            }
        }
        chatMessages.add(UserMessage.from(request.getQuestion()));

        StringBuilder fullResponse = new StringBuilder();
        QwenChatMode mode = resolveMode(request);
        try {
            qwenChatService.streamChat(chatMessages, mode, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String token) {
                    sendEvent(emitter, "token", token, fullResponse);
                }

                @Override
                public void onPartialThinking(PartialThinking thinking) {
                    sendEvent(emitter, "thinking", thinking.text(), null);
                }

                @Override
                public void onCompleteResponse(ChatResponse response) {
                    stopHeartbeat(heartbeatThread);
                    completeConversation(dialogue, historyMessages, request.getQuestion(),
                            fullResponse.toString(), emitter);
                }

                @Override
                public void onError(Throwable error) {
                    stopHeartbeat(heartbeatThread);
                    sendError(emitter, error);
                }
            });
        } catch (RuntimeException e) {
            stopHeartbeat(heartbeatThread);
            sendError(emitter, e);
        }

        return emitter;
    }

    private QwenChatMode resolveMode(StreamRequest request) {
        if (request.isEnableDeepThinking()) {
            return QwenChatMode.DEEP_THINKING;
        }
        if (request.isEnableOnlineSearch()) {
            return QwenChatMode.ONLINE_SEARCH;
        }
        return QwenChatMode.DEFAULT;
    }

    private AIAssistant getAndValidateOwnership(String assistantId, String userId) {
        AIAssistant assistant = assistantMapper.selectById(assistantId);
        if (assistant == null) throw new BaseException(ErrorEnum.AI_ASSISTANT_NOT_FOUND);
        if (!userId.equals(assistant.getUserId())) throw new BaseException(ErrorEnum.ASSISTANT_ACCESS_DENIED);
        return assistant;
    }

    private Dialogue findDialogue(String assistantId) {
        Dialogue dialogue = dialogueMapper.selectOne(
                new LambdaQueryWrapper<Dialogue>()
                        .eq(Dialogue::getAiAssistantId, assistantId)
                        .orderByDesc(Dialogue::getUpdatedAt)
                        .last("LIMIT 1"));
        if (dialogue == null) {
            dialogue = new Dialogue();
            dialogue.setAiAssistantId(assistantId);
            dialogue.setContexts(toJson(List.of()));
            dialogue.setCreatedAt(LocalDateTime.now());
            dialogue.setUpdatedAt(LocalDateTime.now());
            dialogueMapper.insert(dialogue);
        }
        return dialogue;
    }

    private String buildSystemPrompt(AIAssistant assistant) {
        StringBuilder sb = new StringBuilder();
        if (assistant.getAssistantCharacter() != null && !assistant.getAssistantCharacter().isBlank()) {
            sb.append("你是 ").append(assistant.getName()).append("。");
            sb.append(assistant.getAssistantCharacter());
        } else {
            sb.append("你是 ").append(assistant.getName()).append("，是一个有帮助的AI助手。");
        }
        return sb.toString();
    }

    private void sendEvent(SseEmitter emitter, String event, String data, StringBuilder fullResponse) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            log.warn("SSE send {} failed: {}", event, e.getMessage());
        }
        if (fullResponse != null) fullResponse.append(data);
    }

    private void sendError(SseEmitter emitter, Throwable error) {
        String msg = error.getMessage() != null ? error.getMessage() : "LLM调用失败";
        try {
            emitter.send(SseEmitter.event().name("error").data(msg));
        } catch (IOException e) {
            log.warn("SSE send error failed: {}", e.getMessage());
        }
        emitter.completeWithError(error);
    }

    private void completeConversation(Dialogue dialogue, List<List<String>> history,
                                       String question, String answer, SseEmitter emitter) {
        List<List<String>> newHistory = new ArrayList<>(history);
        newHistory.add(List.of("USER", question));
        newHistory.add(List.of("ASSISTANT", answer));
        dialogue.setContexts(toJson(newHistory));
        dialogue.setUpdatedAt(LocalDateTime.now());
        dialogueMapper.updateById(dialogue);
        try {
            emitter.send(SseEmitter.event().name("done").data("complete"));
            emitter.complete();
        } catch (IOException e) {
            log.warn("SSE send done failed: {}", e.getMessage());
        }
    }

    private Thread startHeartbeat(SseEmitter emitter) {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    log.debug("SSE heartbeat endpoint disconnected");
                    break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void stopHeartbeat(Thread thread) {
        if (thread != null && thread.isAlive()) thread.interrupt();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization failed: {}", e.getMessage(), e);
            return "[]";
        }
    }

    private List<List<String>> parseContexts(String contextsJson) {
        try {
            if (contextsJson == null || contextsJson.isBlank()) return new ArrayList<>();
            return objectMapper.readValue(contextsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Parse conversation context failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
