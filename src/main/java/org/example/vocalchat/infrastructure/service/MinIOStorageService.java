package org.example.vocalchat.infrastructure.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOStorageService {

    private static final String BUCKET_NAME = "knowledge-base";

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    public String upload(MultipartFile file, String userId, String kbId) {
        ensureBucketExists();

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storageKey = userId + "/" + kbId + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(storageKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("文件上传成功: bucket={}, key={}, size={}", BUCKET_NAME, storageKey, file.getSize());
            return storageKey;
        } catch (Exception e) {
            log.error("文件上传失败: key={}", storageKey, e);
            throw new BaseException(ErrorEnum.FILE_UPLOAD_FAILED);
        }
    }

    public void delete(String storageKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(storageKey)
                    .build());
            log.info("文件删除成功: bucket={}, key={}", BUCKET_NAME, storageKey);
        } catch (Exception e) {
            log.error("文件删除失败: key={}", storageKey, e);
        }
    }

    public String getPresignedUrl(String storageKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(storageKey)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.error("生成预签名URL失败: key={}", storageKey, e);
            return null;
        }
    }

    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(BUCKET_NAME)
                    .build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(BUCKET_NAME)
                        .build());
                log.info("MinIO Bucket 创建成功: {}", BUCKET_NAME);
            }
        } catch (Exception e) {
            log.error("MinIO Bucket 初始化失败: {}", BUCKET_NAME, e);
            throw new BaseException(ErrorEnum.SYSTEM_ERROR);
        }
    }
}
