package org.example.vocalchat.infrastructure.external.storage;

import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorageService {

    StorageObjectInfo putObject(String objectKey, InputStream inputStream, long size, String contentType);

    InputStream getObject(String objectKey);

    void deleteObject(String objectKey);

    boolean objectExists(String objectKey);

    String createPresignedGetUrl(String objectKey);

    String createPresignedGetUrl(String objectKey, Duration expiry);
}
