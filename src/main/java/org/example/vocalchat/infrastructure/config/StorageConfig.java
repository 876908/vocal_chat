package org.example.vocalchat.infrastructure.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import io.minio.MinioClient;
import org.example.vocalchat.infrastructure.config.properties.StorageProperties;
import org.example.vocalchat.infrastructure.external.storage.CosStorageService;
import org.example.vocalchat.infrastructure.external.storage.MinioStorageService;
import org.example.vocalchat.infrastructure.external.storage.ObjectStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration  //标识这是一个配置类，spring容器启动时会加载他
@EnableConfigurationProperties(StorageProperties.class)  // 开启配置属性绑定，让yml中以vocal-chat.storage 开头的配置自动映射到 StorageProperties 对象中，供后续方法使用
public class StorageConfig {

    @Bean(destroyMethod = "close") //告诉 Spring 在容器关闭时调用 minioClient.close()，安全释放底层网络连接池，防止应用重启时连接泄露。
    @ConditionalOnProperty(prefix = "vocal-chat.storage", name = "provider", havingValue = "minio", matchIfMissing = true)
    public MinioClient minioClient(StorageProperties properties) {
        StorageProperties.Minio minio = properties.getMinio();
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(minio.getEndpoint());

        if (StringUtils.hasText(minio.getAccessKey()) && StringUtils.hasText(minio.getSecretKey())) {
            builder.credentials(minio.getAccessKey(), minio.getSecretKey());
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "vocal-chat.storage", name = "provider", havingValue = "minio", matchIfMissing = true)
    public ObjectStorageService minioStorageService(MinioClient minioClient, StorageProperties properties) {
        return new MinioStorageService(minioClient, properties);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "vocal-chat.storage", name = "provider", havingValue = "cos")
    public COSClient cosClient(StorageProperties properties) {
        StorageProperties.Cos cos = properties.getCos();
        COSCredentials credentials = new BasicCOSCredentials(cos.getSecretId(), cos.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cos.getRegion()));
        return new COSClient(credentials, clientConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "vocal-chat.storage", name = "provider", havingValue = "cos")
    public ObjectStorageService cosStorageService(COSClient cosClient, StorageProperties properties) {
        return new CosStorageService(cosClient, properties);
    }
}
