package org.example.vocalchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.annotation.AutoResult;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.dto.request.CreateKnowledgeBaseRequest;
import org.example.vocalchat.dto.request.UpdateKnowledgeBaseRequest;
import org.example.vocalchat.dto.response.KnowledgeBaseFileVO;
import org.example.vocalchat.dto.response.KnowledgeBaseVO;
import org.example.vocalchat.service.KnowledgeBaseFileService;
import org.example.vocalchat.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@AutoResult
@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseFileService knowledgeBaseFileService;

    // ==================== 知识库 CRUD ====================

    @PostMapping
    public void create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        knowledgeBaseService.create(UserContext.getUserId(), request);
    }

    @GetMapping
    public List<KnowledgeBaseVO> list() {
        return knowledgeBaseService.list(UserContext.getUserId());
    }

    @GetMapping("/{id}")
    public KnowledgeBaseVO getById(@PathVariable String id) {
        return knowledgeBaseService.getById(UserContext.getUserId(), id);
    }

    @PutMapping("/{id}")
    public void update(@PathVariable String id,
                       @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        knowledgeBaseService.update(UserContext.getUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        knowledgeBaseService.delete(UserContext.getUserId(), id);
    }

    // ==================== 文件管理 ====================

    @PostMapping("/{id}/file")
    public KnowledgeBaseFileVO uploadFile(@PathVariable String id,
                                          @RequestParam("file") MultipartFile file) {
        return knowledgeBaseFileService.upload(UserContext.getUserId(), id, file);
    }

    @GetMapping("/{id}/file")
    public List<KnowledgeBaseFileVO> listFiles(@PathVariable String id) {
        return knowledgeBaseFileService.list(UserContext.getUserId(), id);
    }

    @DeleteMapping("/{id}/file/{fileId}")
    public void deleteFile(@PathVariable String id,
                           @PathVariable String fileId) {
        knowledgeBaseFileService.delete(UserContext.getUserId(), id, fileId);
    }

    @GetMapping("/{id}/file/{fileId}/status")
    public KnowledgeBaseFileVO getFileStatus(@PathVariable String id,
                                             @PathVariable String fileId) {
        return knowledgeBaseFileService.getStatus(UserContext.getUserId(), id, fileId);
    }
}
