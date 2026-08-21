package org.example.vocalchat.infrastructure.external.storage;

import com.qcloud.cos.COS;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.infrastructure.config.properties.StorageProperties;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class CosStorageService implements ObjectStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final COS cosClient;
    private final StorageProperties properties;

    public CosStorageService(COS cosClient, StorageProperties properties) {
        this.cosClient = cosClient;
        this.properties = properties;
    }

    @Override
    public StorageObjectInfo putObject(String objectKey, InputStream inputStream, long size, String contentType) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);
        String normalizedContentType = contentType(contentType);

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (size >= 0) {
                metadata.setContentLength(size);
            }
            metadata.setContentType(normalizedContentType);

            PutObjectRequest request = new PutObjectRequest(bucketName(), normalizedKey, inputStream, metadata);
            PutObjectResult result = cosClient.putObject(request);

            return new StorageObjectInfo(
                    StorageProvider.COS,
                    bucketName(),
                    normalizedKey,
                    size,
                    normalizedContentType,
                    result.getETag(),
                    StorageObjectKeys.publicUrl(properties.getCos().getPublicEndpoint(), normalizedKey)
            );
        } catch (RuntimeException e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }

    @Override
    public InputStream getObject(String objectKey) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);

        try {
            COSObject cosObject = cosClient.getObject(bucketName(), normalizedKey);
            return cosObject.getObjectContent();
        } catch (RuntimeException e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }

    @Override
    public void deleteObject(String objectKey) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);

        try {
            cosClient.deleteObject(bucketName(), normalizedKey);
        } catch (RuntimeException e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }

    @Override
    public boolean objectExists(String objectKey) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);

        try {
            return cosClient.doesObjectExist(bucketName(), normalizedKey);
        } catch (RuntimeException e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }

    @Override
    public String createPresignedGetUrl(String objectKey) {
        return createPresignedGetUrl(objectKey, null);
    }

    @Override
    public String createPresignedGetUrl(String objectKey, Duration expiry) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);

        try {
            Duration effectiveExpiry = expiry == null ? properties.getPresignedUrlExpiry() : expiry;
            Date expiration = Date.from(Instant.now().plus(effectiveExpiry));
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    bucketName(),
                    normalizedKey,
                    HttpMethodName.GET
            );
            request.setExpiration(expiration);
            return cosClient.generatePresignedUrl(request).toString();
        } catch (RuntimeException e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }

    private String bucketName() {
        return properties.getBucketName();
    }

    private String contentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : DEFAULT_CONTENT_TYPE;
    }
}
