package org.example.vocalchat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.dto.response.KnowledgeBaseFileVO;
import org.example.vocalchat.entity.KnowledgeBase;
import org.example.vocalchat.entity.KnowledgeBaseFile;
import org.example.vocalchat.infrastructure.service.MinIOStorageService;
import org.example.vocalchat.mapper.KnowledgeBaseFileMapper;
import org.example.vocalchat.mapper.KnowledgeBaseMapper;
import org.example.vocalchat.service.KnowledgeBaseFileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseFileServiceImpl implements KnowledgeBaseFileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "txt", "md", "docx");

    private final KnowledgeBaseFileMapper knowledgeBaseFileMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final MinIOStorageService minIOStorageService;

    @Override
    @Transactional
    public KnowledgeBaseFileVO upload(String userId, String kbId, MultipartFile file) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new BaseException(ErrorEnum.KNOWLEDGE_BASE_NOT_FOUND);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BaseException(ErrorEnum.PARAM_ERROR.getCode(),
                    "不支持的文件类型: " + extension + "，仅支持 PDF/TXT/MD/DOCX");
        }

        String fileId = UUID.randomUUID().toString();
        KnowledgeBaseFile kbFile = KnowledgeBaseFile.builder()
                .id(fileId)
                .knowledgeBaseId(kbId)
                .fileName(originalFilename)
                .fileType(extension.toLowerCase())
                .fileSize(file.getSize())
                .storageKey("")
                .status("UPLOADING")
                .chunkCount(0)
                .build();
        knowledgeBaseFileMapper.insert(kbFile);

        String storageKey;
        try {
            storageKey = minIOStorageService.upload(file, userId, kbId);
        } catch (Exception e) {
            kbFile.setStatus("FAILED");
            knowledgeBaseFileMapper.updateById(kbFile);
            throw e;
        }

        kbFile.setStorageKey(storageKey);
        kbFile.setStatus("COMPLETED");
        knowledgeBaseFileMapper.updateById(kbFile);

        knowledgeBaseMapper.incrementDocumentCount(kbId, 1);

        log.info("知识库文件上传成功: kbId={}, fileId={}, fileName={}, size={}",
                kbId, fileId, originalFilename, file.getSize());

        return toVO(kbFile);
    }

    @Override
    public List<KnowledgeBaseFileVO> list(String userId, String kbId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new BaseException(ErrorEnum.KNOWLEDGE_BASE_NOT_FOUND);
        }

        List<KnowledgeBaseFile> files = knowledgeBaseFileMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseFile>()
                        .eq(KnowledgeBaseFile::getKnowledgeBaseId, kbId));
        return files.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(String userId, String kbId, String fileId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new BaseException(ErrorEnum.KNOWLEDGE_BASE_NOT_FOUND);
        }

        KnowledgeBaseFile kbFile = knowledgeBaseFileMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBaseFile>()
                        .eq(KnowledgeBaseFile::getKnowledgeBaseId, kbId)
                        .eq(KnowledgeBaseFile::getId, fileId));
        if (kbFile == null) {
            throw new BaseException(ErrorEnum.PARAM_ERROR.getCode(), "文件不存在");
        }

        minIOStorageService.delete(kbFile.getStorageKey());

        knowledgeBaseFileMapper.deleteById(fileId);

        knowledgeBaseMapper.incrementDocumentCount(kbId, -1);

        log.info("知识库文件删除成功: kbId={}, fileId={}, fileName={}", kbId, fileId, kbFile.getFileName());
    }

    @Override
    public KnowledgeBaseFileVO getStatus(String userId, String kbId, String fileId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new BaseException(ErrorEnum.KNOWLEDGE_BASE_NOT_FOUND);
        }

        KnowledgeBaseFile kbFile = knowledgeBaseFileMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeBaseFile>()
                        .eq(KnowledgeBaseFile::getKnowledgeBaseId, kbId)
                        .eq(KnowledgeBaseFile::getId, fileId));
        if (kbFile == null) {
            throw new BaseException(ErrorEnum.PARAM_ERROR.getCode(), "文件不存在");
        }

        return toVO(kbFile);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private KnowledgeBaseFileVO toVO(KnowledgeBaseFile file) {
        return KnowledgeBaseFileVO.builder()
                .id(file.getId())
                .knowledgeBaseId(file.getKnowledgeBaseId())
                .fileName(file.getFileName())
                .fileType(file.getFileType())
                .fileSize(file.getFileSize())
                .status(file.getStatus())
                .chunkCount(file.getChunkCount())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }
}
