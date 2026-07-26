package org.example.vocalchat.controller;

import jakarta.validation.Valid;
import org.example.vocalchat.common.result.BaseResult;
import org.example.vocalchat.dto.request.CreateAssistantRequest;
import org.example.vocalchat.dto.request.ModifyAssistantConfigRequest;
import org.example.vocalchat.dto.request.StreamRequest;
import org.example.vocalchat.dto.response.AIAssistantVO;
import org.example.vocalchat.service.AIAssistantService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/aiAssistant")
public class AIAssistantController {

    private final AIAssistantService assistantService;

    public AIAssistantController(AIAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/createNewAssistant")
    public BaseResult<String> createNewAssistant(@Valid @RequestBody CreateAssistantRequest request) {
        return BaseResult.success(assistantService.createAssistant(request));
    }

    @GetMapping("/aiAssistants")
    public BaseResult<List<AIAssistantVO>> aiAssistants() {
        return BaseResult.success(assistantService.listAssistants());
    }

    @PostMapping("/modifyAssistantConfig")
    public BaseResult<Void> modifyAssistantConfig(@Valid @RequestBody ModifyAssistantConfigRequest request) {
        assistantService.modifyConfig(request);
        return BaseResult.success();
    }

    @DeleteMapping("/deleteAssistant")
    public BaseResult<Void> deleteAssistant(@RequestParam String aiAssistantId) {
        assistantService.deleteAssistant(aiAssistantId);
        return BaseResult.success();
    }

    @GetMapping("/{aiAssistantId}/conversation-log")
    public BaseResult<List<List<String>>> conversationLog(@PathVariable String aiAssistantId) {
        return BaseResult.success(assistantService.getConversationLog(aiAssistantId));
    }

    @DeleteMapping("/{aiAssistantId}/conversation-log")
    public BaseResult<Void> resetConversation(@PathVariable String aiAssistantId) {
        assistantService.resetConversation(aiAssistantId);
        return BaseResult.success();
    }

    @PostMapping(value = "/streamGenerateReply", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGenerateReply(@Valid @RequestBody StreamRequest request) {
        return assistantService.streamGenerateReply(request);
    }
}
