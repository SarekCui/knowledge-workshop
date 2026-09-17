package com.knowledge.iam.profile.storage;

import com.knowledge.storage.core.ObjectStorage;
import com.knowledge.storage.minio.MinioObjectStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "knowledge.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AvatarStorageConfiguration {

    @Bean
    ObjectStorage objectStorage(
            @Value("${knowledge.storage.endpoint}") String endpoint,
            @Value("${knowledge.storage.access-key}") String accessKey,
            @Value("${knowledge.storage.secret-key}") String secretKey,
            @Value("${knowledge.storage.bucket}") String bucket) {
        return MinioObjectStorage.create(endpoint, accessKey, secretKey, bucket);
    }
}
