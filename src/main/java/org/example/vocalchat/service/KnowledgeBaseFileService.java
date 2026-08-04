package org.example.vocalchat.service;

import org.example.vocalchat.dto.response.KnowledgeBaseFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeBaseFileService {

    KnowledgeBaseFileVO upload(String userId, String kbId, MultipartFile file);

    List<KnowledgeBaseFileVO> list(String userId, String kbId);

    void delete(String userId, String kbId, String fileId);

    KnowledgeBaseFileVO getStatus(String userId, String kbId, String fileId);
}
