package org.example.vocalchat.service;

import org.example.vocalchat.dto.request.CreateKnowledgeBaseRequest;
import org.example.vocalchat.dto.request.UpdateKnowledgeBaseRequest;
import org.example.vocalchat.dto.response.KnowledgeBaseVO;

import java.util.List;

public interface KnowledgeBaseService {

    void create(String userId, CreateKnowledgeBaseRequest request);

    List<KnowledgeBaseVO> list(String userId);

    KnowledgeBaseVO getById(String userId, String kbId);

    void update(String userId, String kbId, UpdateKnowledgeBaseRequest request);

    void delete(String userId, String kbId);
}
