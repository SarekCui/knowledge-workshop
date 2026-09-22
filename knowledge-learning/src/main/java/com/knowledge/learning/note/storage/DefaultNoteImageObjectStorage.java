package com.knowledge.learning.note.storage;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.note.bo.NormalizedNoteImageBO;
import com.knowledge.storage.core.ObjectStorage;
import com.knowledge.storage.exception.ObjectStorageException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "knowledge.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DefaultNoteImageObjectStorage implements NoteImageObjectStorage {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultNoteImageObjectStorage.class);
    private static final Duration URL_VALIDITY = Duration.ofHours(1);

    @Resource
    private ObjectStorage objectStorage;

    @Override
    public String put(NormalizedNoteImageBO image) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String objectKey = "note-images/%d/%02d/%s.%s".formatted(today.getYear(), today.getMonthValue(),
                UUID.randomUUID(), image.extension());
        try {
            objectStorage.put(objectKey, image.content(), image.contentType());
            return objectKey;
        } catch (ObjectStorageException exception) {
            LOG.error("Note image upload failed", exception);
            throw BusinessException.serviceUnavailable("Note 图片存储暂时不可用");
        }
    }

    @Override
    public String temporaryUrl(String objectKey) {
        try {
            return objectStorage.presignedGetUrl(objectKey, URL_VALIDITY);
        } catch (ObjectStorageException exception) {
            LOG.error("Note image URL signing failed, objectKey={}", objectKey, exception);
            throw BusinessException.serviceUnavailable("Note 图片暂时不可访问");
        }
    }

    @Override
    public boolean delete(String objectKey) {
        try {
            objectStorage.delete(objectKey);
            return true;
        } catch (ObjectStorageException exception) {
            LOG.warn("Note image cleanup failed, objectKey={}", objectKey, exception);
            return false;
        }
    }
}
