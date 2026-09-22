package com.knowledge.iam.profile.storage;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.iam.profile.bo.AvatarImageBO;
import com.knowledge.storage.core.ObjectStorage;
import com.knowledge.storage.exception.ObjectStorageException;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "knowledge.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DefaultAvatarObjectStorage implements AvatarObjectStorage {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAvatarObjectStorage.class);
    private static final Duration URL_VALIDITY = Duration.ofHours(1);

    @Resource
    private ObjectStorage objectStorage;

    @Override
    public String put(AvatarImageBO image) {
        String objectKey = "avatars/" + UUID.randomUUID() + "." + image.extension();
        try {
            objectStorage.put(objectKey, image.content(), image.contentType());
            return objectKey;
        } catch (ObjectStorageException exception) {
            LOG.error("Avatar object upload failed", exception);
            throw BusinessException.serviceUnavailable("头像存储暂时不可用");
        }
    }

    @Override
    public String temporaryUrl(String objectKey) {
        try {
            return objectStorage.presignedGetUrl(objectKey, URL_VALIDITY);
        } catch (ObjectStorageException exception) {
            LOG.warn("Avatar URL signing failed, objectKey={}", objectKey, exception);
            return null;
        }
    }

    @Override
    public void deleteQuietly(String objectKey) {
        try {
            objectStorage.delete(objectKey);
        } catch (ObjectStorageException exception) {
            LOG.warn("Avatar cleanup failed, objectKey={}", objectKey, exception);
        }
    }
}
