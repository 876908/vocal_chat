package org.example.vocalchat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.annotation.AutoResult;
import org.example.vocalchat.common.annotation.LogOperation;
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


    @LogOperation("创建知识库")
    @PostMapping
    public void create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        knowledgeBaseService.create(UserContext.getUserId(), request);
    }

    @LogOperation("查询知识库列表")
    @GetMapping
    public List<KnowledgeBaseVO> list() {
        return knowledgeBaseService.list(UserContext.getUserId());
    }

    @LogOperation("查询知识库详情")
    @GetMapping("/{id}")
    public KnowledgeBaseVO getById(@PathVariable String id) {
        return knowledgeBaseService.getById(UserContext.getUserId(), id);
    }

    @LogOperation("更新知识库")
    @PutMapping("/{id}")
    public void update(@PathVariable String id,
                       @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        knowledgeBaseService.update(UserContext.getUserId(), id, request);
    }

    @LogOperation("删除知识库")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        knowledgeBaseService.delete(UserContext.getUserId(), id);
    }


    @LogOperation("上传知识库文件")
    @PostMapping("/{id}/file")
    public KnowledgeBaseFileVO uploadFile(@PathVariable String id,
                                          @RequestParam("file") MultipartFile file) {
        return knowledgeBaseFileService.upload(UserContext.getUserId(), id, file);
    }

    @LogOperation("查询知识库文件列表")
    @GetMapping("/{id}/file")
    public List<KnowledgeBaseFileVO> listFiles(@PathVariable String id) {
        return knowledgeBaseFileService.list(UserContext.getUserId(), id);
    }

    @LogOperation("删除知识库文件")
    @DeleteMapping("/{id}/file/{fileId}")
    public void deleteFile(@PathVariable String id,
                           @PathVariable String fileId) {
        knowledgeBaseFileService.delete(UserContext.getUserId(), id, fileId);
    }

    @LogOperation("查询文件处理状态")
    @GetMapping("/{id}/file/{fileId}/status")
    public KnowledgeBaseFileVO getFileStatus(@PathVariable String id,
                                             @PathVariable String fileId) {
        return knowledgeBaseFileService.getStatus(UserContext.getUserId(), id, fileId);
    }
}
