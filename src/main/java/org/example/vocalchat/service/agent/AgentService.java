package org.example.vocalchat.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.dto.request.StreamRequest;
import org.example.vocalchat.dto.response.AgentResultVO;
import org.example.vocalchat.dto.response.AgentStepVO;
import org.example.vocalchat.dto.response.ToolVO;
import org.example.vocalchat.entity.AIAssistant;
import org.example.vocalchat.entity.Dialogue;
import org.example.vocalchat.infrastructure.external.llm.QwenChatMode;
import org.example.vocalchat.infrastructure.external.llm.QwenChatService;
import org.example.vocalchat.mapper.AIAssistantMapper;
import org.example.vocalchat.mapper.DialogueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Agent 编排服务：实现 Agent Loop 核心控制流。
 *
 * <p>与普通对话的区别：LLM 的每次响应可能是文本，也可能是工具调用请求。
 * 本类循环执行「LLM 决策 → 执行工具 → 结果回传」直到 LLM 输出最终文本回复，
 * 并通过 SSE 推送 {@code agent_thinking / agent_tool_call / agent_tool_result}
 * 三个 Agent 专用事件，实现执行链路可视化。</p>
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final long SSE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long HEARTBEAT_INTERVAL_MS = 15000;
    private static final int MAX_ITERATIONS = 10;   // 安全阀，防止 LLM 陷入工具调用死循环

    private final AIAssistantMapper assistantMapper;
    private final DialogueMapper dialogueMapper;
    private final QwenChatService qwenChatService;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public AgentService(AIAssistantMapper assistantMapper,
                        DialogueMapper dialogueMapper,
                        QwenChatService qwenChatService,
                        ToolRegistry toolRegistry,
                        ObjectMapper objectMapper) {
        this.assistantMapper = assistantMapper;
        this.dialogueMapper = dialogueMapper;
        this.qwenChatService = qwenChatService;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * Agent 流式执行（SSE）。
     *
     * <p>Agent Loop 使用同步 {@code chat()}，在虚拟线程中执行，避免阻塞请求线程；
     * 同时在虚拟线程中恢复 {@link UserContext}，使工具方法内部能通过
     * {@code UserContext.requireUserId()} 获取当前用户。</p>
     */
    public SseEmitter agentStream(StreamRequest request) {
        String userId = UserContext.requireUserId();
        String token = UserContext.getToken();
        AIAssistant assistant = getAndValidateOwnership(request.getAiAssistantId(), userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        // 取消标志：SSE 断开/超时/发送失败时置 true，Agent Loop 与心跳线程据此停止
        AtomicBoolean cancelled = new AtomicBoolean(false);
        // 保存 Agent 线程引用，供 onTimeout/onCompletion 回调中断执行
        AtomicReference<Thread> agentThreadRef = new AtomicReference<>();

        emitter.onCompletion(() -> cancel(agentThreadRef, cancelled, "SSE 连接完成"));
        emitter.onTimeout(() -> cancel(agentThreadRef, cancelled, "SSE 超时"));

        Thread agentThread = Thread.startVirtualThread(() -> {
            UserContext.set(userId, token);
            try {
                runAgentLoop(assistant, request, emitter, cancelled);
            } catch (Exception e) {
                if (!cancelled.get()) {
                    sendError(emitter, e, cancelled);
                } else {
                    log.debug("Agent Loop 已取消，忽略异常: {}", e.getMessage());
                }
            } finally {
                UserContext.clear();
            }
        });
        agentThreadRef.set(agentThread);

        return emitter;
    }

    /** 取消 Agent 执行：置取消标志并中断 Agent 线程。 */
    private void cancel(AtomicReference<Thread> agentThreadRef, AtomicBoolean cancelled, String reason) {
        if (cancelled.compareAndSet(false, true)) {
            log.info("取消 Agent 执行: {}", reason);
        }
        Thread agentThread = agentThreadRef.get();
        if (agentThread != null && agentThread != Thread.currentThread()) {
            agentThread.interrupt();
        }
    }

    /**
     * Agent 同步执行：跑完整个 Agent Loop 后一次性返回结果（对应 api.md 4.3 的 /agentRun）。
     *
     * <p>与 {@link #agentStream} 的差别仅在"结果怎么交付"：这里把每一步收集进 steps 列表，
     * 最终返回 {@link AgentResultVO}，而不是通过 SSE 实时推送。Loop 逻辑完全一致。</p>
     */
    public AgentResultVO run(StreamRequest request) {
        String userId = UserContext.requireUserId();
        AIAssistant assistant = getAndValidateOwnership(request.getAiAssistantId(), userId);

        List<AgentStepVO> steps = new ArrayList<>();
        List<ChatMessage> messages = buildInitialMessages(assistant, request);

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            steps.add(thinkingStep(iteration));

            ChatResponse response = qwenChatService.chat(
                    ChatRequest.builder()
                            .messages(messages)
                            .toolSpecifications(toolRegistry.allSpecifications())
                            .build(),
                    resolveMode(request));
            AiMessage aiMessage = response.aiMessage();

            if (aiMessage.hasToolExecutionRequests()) {
                messages.add(aiMessage);
                for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                    steps.add(toolCallStep(toolRequest));

                    ToolRegistry.ToolResult toolResult = toolRegistry.execute(toolRequest);
                    steps.add(toolResultStep(toolRequest.name(), toolResult));

                    messages.add(ToolExecutionResultMessage.builder()
                            .id(toolRequest.id())
                            .toolName(toolRequest.name())
                            .text(toolResult.text())
                            .isError(!toolResult.success())
                            .build());
                }
                continue;
            }

            String finalAnswer = aiMessage.text();
            persistConversation(assistant, request.getQuestion(), finalAnswer);

            AgentResultVO result = new AgentResultVO();
            result.setFinalAnswer(finalAnswer);
            result.setSuccess(true);
            result.setSteps(steps);
            return result;
        }

        throw new BaseException(ErrorEnum.AGENT_LOOP_EXCEEDED);
    }

    /** 可用工具列表（对应 api.md 4.3 的 GET /tools）。 */
    public List<ToolVO> listTools() {
        return toolRegistry.allSpecifications().stream()
                .map(this::toToolVO)
                .toList();
    }

    private ToolVO toToolVO(ToolSpecification spec) {
        ToolVO vo = new ToolVO();
        vo.setType("tool");
        vo.setTool(spec.name());
        vo.setContent(spec.description());
        return vo;
    }

    private AgentStepVO thinkingStep(int iteration) {
        AgentStepVO step = new AgentStepVO();
        step.setType("thinking");
        step.setContent("正在分析任务（第 " + (iteration + 1) + " 轮）...");
        step.setTimestamp(System.currentTimeMillis());
        return step;
    }

    private AgentStepVO toolCallStep(ToolExecutionRequest request) {
        AgentStepVO step = new AgentStepVO();
        step.setType("tool_call");
        step.setTool(request.name());
        step.setArgs(parseArguments(request.arguments()));
        step.setTimestamp(System.currentTimeMillis());
        return step;
    }

    private AgentStepVO toolResultStep(String toolName, ToolRegistry.ToolResult toolResult) {
        AgentStepVO step = new AgentStepVO();
        step.setType("tool_result");
        step.setTool(toolName);
        step.setResult(toolResult.text());
        step.setSuccess(toolResult.success());
        step.setTimestamp(System.currentTimeMillis());
        return step;
    }

    /** Agent Loop 主循环。 */
    private void runAgentLoop(AIAssistant assistant, StreamRequest request, SseEmitter emitter, AtomicBoolean cancelled) {
        sendEvent(emitter, "started", "invoked", cancelled);
        Thread heartbeatThread = startHeartbeat(emitter, cancelled);

        try {
            List<ChatMessage> messages = buildInitialMessages(assistant, request);

            for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                if (cancelled.get()) {
                    return;
                }
                sendEvent(emitter, "agent_thinking", "正在分析任务（第 " + (iteration + 1) + " 轮）...", cancelled);

                ChatResponse response = qwenChatService.chat(
                        ChatRequest.builder()
                                .messages(messages)
                                .toolSpecifications(toolRegistry.allSpecifications())
                                .build(),
                        resolveMode(request));
                AiMessage aiMessage = response.aiMessage();

                // 分支 1：LLM 请求调用工具
                if (aiMessage.hasToolExecutionRequests()) {
                    messages.add(aiMessage);   // 先把"要调用工具"的 AI 消息加入上下文
                    for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                        if (cancelled.get()) {
                            return;
                        }
                        sendToolCall(emitter, toolRequest, cancelled);
                        ToolRegistry.ToolResult toolResult = toolRegistry.execute(toolRequest);
                        sendToolResult(emitter, toolRequest.name(), toolResult.success(), toolResult.text(), cancelled);
                        // 工具结果作为 ToolExecutionResultMessage 回传，供 LLM 下一轮决策；
                        // 通过 isError 标记失败，让 LLM 知道工具出错而非正常返回
                        messages.add(ToolExecutionResultMessage.builder()
                                .id(toolRequest.id())
                                .toolName(toolRequest.name())
                                .text(toolResult.text())
                                .isError(!toolResult.success())
                                .build());
                    }
                    continue;
                }

                // 分支 2：LLM 输出纯文本，任务完成
                if (cancelled.get()) {
                    return;
                }
                String finalAnswer = aiMessage.text();
                sendEvent(emitter, "token", finalAnswer, cancelled);
                persistConversation(assistant, request.getQuestion(), finalAnswer);
                sendEvent(emitter, "done", "complete", cancelled);
                emitter.complete();
                return;
            }

            // 超过最大轮次仍未收敛，强制终止
            sendError(emitter, new BaseException(ErrorEnum.AGENT_LOOP_EXCEEDED), cancelled);
        } finally {
            stopHeartbeat(heartbeatThread);
        }
    }

    /** 构建初始消息：System Prompt（角色设定 + 工具使用规则）+ 历史对话 + 用户问题。 */
    private List<ChatMessage> buildInitialMessages(AIAssistant assistant, StreamRequest request) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(buildSystemPrompt(assistant)));

        List<List<String>> history = parseContexts(findDialogue(assistant.getId()).getContexts());
        for (List<String> entry : history) {
            if (entry.size() < 2) {
                continue;
            }
            String role = entry.get(0);
            String content = entry.get(1);
            if ("USER".equalsIgnoreCase(role)) {
                messages.add(UserMessage.from(content));
            } else if ("ASSISTANT".equalsIgnoreCase(role)) {
                messages.add(AiMessage.from(content));
            }
        }
        messages.add(UserMessage.from(request.getQuestion()));
        return messages;
    }

    /** System Prompt：助手角色设定 + 告知 LLM 工具使用规则。 */
    private String buildSystemPrompt(AIAssistant assistant) {
        StringBuilder sb = new StringBuilder();
        if (assistant.getAssistantCharacter() != null && !assistant.getAssistantCharacter().isBlank()) {
            sb.append("你是 ").append(assistant.getName()).append("。").append(assistant.getAssistantCharacter());
        } else {
            sb.append("你是 ").append(assistant.getName()).append("，是一个有帮助的 AI 助手。");
        }
        sb.append("\n\n你可以调用提供的工具来完成任务。根据用户需求自主选择合适的工具，"
                + "工具执行完成后根据返回结果决定下一步，直到任务完成。"
                + "任务完成后必须用文字回复用户，不要停留在工具调用。");
        return sb.toString();
    }

    /** SSE 事件：开始调用工具。 */
    private void sendToolCall(SseEmitter emitter, ToolExecutionRequest request, AtomicBoolean cancelled) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tool", request.name());
        payload.put("args", parseArguments(request.arguments()));
        sendEvent(emitter, "agent_tool_call", toJson(payload), cancelled);
    }

    /** SSE 事件：工具执行结果。 */
    private void sendToolResult(SseEmitter emitter, String toolName, boolean success, String summary, AtomicBoolean cancelled) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tool", toolName);
        payload.put("success", success);
        payload.put("summary", summary);
        sendEvent(emitter, "agent_tool_result", toJson(payload), cancelled);
    }

    /** 把工具调用的 JSON 参数字符串解析为对象（失败时保留原始字符串）。 */
    private Object parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(arguments, Object.class);
        } catch (JsonProcessingException e) {
            log.warn("解析工具参数失败: {}", e.getMessage());
            return arguments;
        }
    }

    /** 持久化最终问答到 dialogue 表（与普通对话一致）。 */
    private void persistConversation(AIAssistant assistant, String question, String answer) {
        Dialogue dialogue = findDialogue(assistant.getId());
        List<List<String>> history = parseContexts(dialogue.getContexts());
        history.add(List.of("USER", question));
        history.add(List.of("ASSISTANT", answer));
        dialogue.setContexts(toJson(history));
        dialogue.setUpdatedAt(LocalDateTime.now());
        dialogueMapper.updateById(dialogue);
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
        if (assistant == null) {
            throw new BaseException(ErrorEnum.AI_ASSISTANT_NOT_FOUND);
        }
        if (!userId.equals(assistant.getUserId())) {
            throw new BaseException(ErrorEnum.ASSISTANT_ACCESS_DENIED);
        }
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

    // ==== SSE / JSON 工具方法（与 AIAssistantService 保持一致）====

    /**
     * 发送 SSE 事件；发送失败（如浏览器断开）时置取消标志，让 Agent Loop 尽快停止。
     */
    private void sendEvent(SseEmitter emitter, String event, String data, AtomicBoolean cancelled) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException e) {
            log.warn("SSE send {} failed: {}", event, e.getMessage());
            cancelled.set(true);
        }
    }

    private void sendError(SseEmitter emitter, Throwable error, AtomicBoolean cancelled) {
        String msg = error.getMessage() != null ? error.getMessage() : "Agent 执行失败";
        sendEvent(emitter, "error", msg, cancelled);
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("SSE complete after error failed: {}", e.getMessage());
        }
    }

    private Thread startHeartbeat(SseEmitter emitter, AtomicBoolean cancelled) {
        Thread thread = new Thread(() -> {
            while (!cancelled.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL_MS);
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    log.debug("SSE heartbeat endpoint disconnected");
                    cancelled.set(true);   // 心跳发送失败 → 连接已断，通知 Agent Loop 停止
                    break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private void stopHeartbeat(Thread thread) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败: {}", e.getMessage(), e);
            return "{}";
        }
    }

    private List<List<String>> parseContexts(String contextsJson) {
        try {
            if (contextsJson == null || contextsJson.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(contextsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.warn("解析对话上下文失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
