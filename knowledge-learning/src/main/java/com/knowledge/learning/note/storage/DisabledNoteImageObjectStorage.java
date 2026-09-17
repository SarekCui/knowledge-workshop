package com.knowledge.learning.note.storage;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.learning.note.bo.NormalizedNoteImageBO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "knowledge.storage", name = "enabled", havingValue = "false")
public class DisabledNoteImageObjectStorage implements NoteImageObjectStorage {
    @Override
    public String put(NormalizedNoteImageBO image) {
        throw BusinessException.serviceUnavailable("Note 图片存储未启用");
    }

    @Override
    public String temporaryUrl(String objectKey) {
        throw BusinessException.serviceUnavailable("Note 图片存储未启用");
    }

    @Override
    public boolean delete(String objectKey) {
        return false;
    }
}
