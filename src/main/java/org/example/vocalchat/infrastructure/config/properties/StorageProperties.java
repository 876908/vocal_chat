package org.example.vocalchat.infrastructure.config.properties;

import lombok.Data;
import org.example.vocalchat.infrastructure.external.storage.StorageProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "vocal-chat.storage")
public class StorageProperties {

    private StorageProvider provider = StorageProvider.MINIO;

    private String bucketName = "vocal-chat";

    private Duration presignedUrlExpiry = Duration.ofMinutes(30);

    private Minio minio = new Minio();

    private Cos cos = new Cos();

    @Data
    public static class Minio {

        private String endpoint = "http://localhost:9000";

        private String accessKey;

        private String secretKey;

        private String publicEndpoint;

        private boolean ensureBucketExists = true;
    }

    @Data
    public static class Cos {

        private String region = "ap-guangzhou";

        private String secretId;

        private String secretKey;

        private String publicEndpoint;
    }
}
