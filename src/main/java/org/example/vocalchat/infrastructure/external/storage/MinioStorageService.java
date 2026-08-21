package org.example.vocalchat.infrastructure.external.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.infrastructure.config.properties.StorageProperties;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class MinioStorageService implements ObjectStorageService {
    //未指定类型时默认为二进制流
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    //流大小未知时，分片上传的块大小是10mb
    private static final long UNKNOWN_SIZE_PART_SIZE = 10L * 1024L * 1024L;
    //最大有效期7天
    private static final long MAX_PRESIGNED_SECONDS = 7L * 24L * 60L * 60L;

    private final MinioClient minioClient;
    private final StorageProperties properties;
    private volatile boolean bucketChecked;

    public MinioStorageService(MinioClient minioClient, StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }
    // 上传文件
    @Override
    public StorageObjectInfo putObject(String objectKey, InputStream inputStream, long size, String contentType) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);
        String normalizedContentType = contentType(contentType);

        try {
            ensureBucketExists();
            ObjectWriteResponse response = minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName())
                            .object(normalizedKey)
                            .stream(inputStream, size, partSize(size))
                            .contentType(normalizedContentType)
                            .build()
            );

            return new StorageObjectInfo(
                    StorageProvider.MINIO,
                    bucketName(),
                    normalizedKey,
                    size,
                    normalizedContentType,
                    response.etag(), //MinIO返回的校验值
                    StorageObjectKeys.publicUrl(properties.getMinio().getPublicEndpoint(), normalizedKey)
            );
        } catch (Exception e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }
    // 下载文件
    @Override
    public InputStream getObject(String objectKey) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);

        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName())
                            .object(normalizedKey)
                            .build()
            );
        } catch (Exception e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }
    //删除文件
    @Override
    public void deleteObject(String objectKey) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName())
                            .object(normalizedKey)
                            .build()
            );
        } catch (Exception e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }
    //文件存在性检查
    @Override
    public boolean objectExists(String objectKey) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);

        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName())
                            .object(normalizedKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code()) || "NoSuchBucket".equals(e.errorResponse().code())) {
                return false;
            }
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        } catch (Exception e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }
    // 生成临时下载链接
    @Override
    public String createPresignedGetUrl(String objectKey) {
        return createPresignedGetUrl(objectKey, null);
    }

    @Override
    public String createPresignedGetUrl(String objectKey, Duration expiry) {
        String normalizedKey = StorageObjectKeys.normalize(objectKey);

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName())
                            .object(normalizedKey)
                            .method(Method.GET)
                            .expiry(presignedSeconds(expiry), TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new BaseException(ErrorEnum.OBJECT_STORAGE_ERROR, e);
        }
    }
    //仅再第一次上传时触发网络请求
    private void ensureBucketExists() throws Exception {
        if (!properties.getMinio().isEnsureBucketExists() || bucketChecked) {
            return;
        }

        synchronized (this) {
            if (bucketChecked) {
                return;
            }

            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName())
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName())
                                .build()
                );
            }
            bucketChecked = true;
        }
    }

    private String bucketName() {
        return properties.getBucketName();
    }
    // contentType为空则不默认值
    private String contentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : DEFAULT_CONTENT_TYPE;
    }
    // 》0就传入-1，传入-1后MinIO SDK会自动根据总大小选择最优方案，《0就分10mb
    private long partSize(long size) {
        return size >= 0 ? -1L : UNKNOWN_SIZE_PART_SIZE;
    }

    private int presignedSeconds(Duration expiry) {
        Duration effectiveExpiry = expiry == null ? properties.getPresignedUrlExpiry() : expiry;
        long seconds = Math.max(1L, effectiveExpiry.toSeconds());
        return (int) Math.min(seconds, MAX_PRESIGNED_SECONDS);
    }
}
