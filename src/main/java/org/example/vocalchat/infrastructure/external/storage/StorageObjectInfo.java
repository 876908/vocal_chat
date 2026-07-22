package org.example.vocalchat.infrastructure.external.storage;

public record StorageObjectInfo(
        StorageProvider provider,
        String bucketName,
        String objectKey,
        long size,
        String contentType,
        String eTag,
        String publicUrl
) {
}
