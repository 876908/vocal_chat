package org.example.vocalchat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.dto.request.CreateKnowledgeBaseRequest;
import org.example.vocalchat.dto.request.UpdateKnowledgeBaseRequest;
import org.example.vocalchat.dto.response.KnowledgeBaseVO;
import org.example.vocalchat.entity.KnowledgeBase;
import org.example.vocalchat.entity.KnowledgeBaseFile;
import org.example.vocalchat.infrastructure.service.MinIOStorageService;
import org.example.vocalchat.mapper.KnowledgeBaseFileMapper;
import org.example.vocalchat.mapper.KnowledgeBaseMapper;
import org.example.vocalchat.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseFileMapper knowledgeBaseFileMapper;
    private final MinIOStorageService minIOStorageService;

    @Override
    public void create(String userId, CreateKnowledgeBaseRequest request) {
        KnowledgeBase kb = KnowledgeBase.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .status("ACTIVE")
                .documentCount(0)
                .chunkCount(0)
                .build();
        knowledgeBaseMapper.insert(kb);
        log.info("知识库创建成功: id={}, name={}, userId={}", kb.getId(), kb.getName(), userId);
    }

    @Override
    public List<KnowledgeBaseVO> list(String userId) {
        List<KnowledgeBase> kbList = knowledgeBaseMapper.selectByUserId(userId);
        return kbList.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public KnowledgeBaseVO getById(String userId, String kbId) {
        KnowledgeBase kb = findOwned(userId, kbId);
        return toVO(kb);
    }

    @Override
    public void update(String userId, String kbId, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase kb = findOwned(userId, kbId);
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        knowledgeBaseMapper.updateById(kb);
        log.info("知识库更新成功: id={}, name={}", kbId, request.getName());
    }

    @Override
    @Transactional
    public void delete(String userId, String kbId) {
        KnowledgeBase kb = findOwned(userId, kbId);

        // 级联删除所有文件（MinIO + 数据库）
        List<KnowledgeBaseFile> files = knowledgeBaseFileMapper.selectByKnowledgeBaseId(kbId);
        for (KnowledgeBaseFile file : files) {
            minIOStorageService.delete(file.getStorageKey());
            knowledgeBaseFileMapper.deleteById(file.getId());
        }
        log.info("知识库删除: 已删除 {} 个关联文件, kbId={}", files.size(), kbId);

        knowledgeBaseMapper.deleteById(kbId);
        log.info("知识库删除成功: id={}, name={}", kbId, kb.getName());
    }

    /**
     * 查找属于当前用户的知识库，不存在或无权限则抛异常。
     */
    private KnowledgeBase findOwned(String userId, String kbId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BaseException(ErrorEnum.KNOWLEDGE_BASE_NOT_FOUND);
        }
        if (!kb.getUserId().equals(userId)) {
            throw new BaseException(ErrorEnum.KNOWLEDGE_BASE_NOT_FOUND);
        }
        return kb;
    }

    private KnowledgeBaseVO toVO(KnowledgeBase kb) {
        return KnowledgeBaseVO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .status(kb.getStatus())
                .documentCount(kb.getDocumentCount())
                .chunkCount(kb.getChunkCount())
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }
}
