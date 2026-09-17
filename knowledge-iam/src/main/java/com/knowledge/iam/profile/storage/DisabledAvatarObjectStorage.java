package com.knowledge.iam.profile.storage;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.iam.profile.bo.AvatarImageBO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "knowledge.storage", name = "enabled", havingValue = "false")
public class DisabledAvatarObjectStorage implements AvatarObjectStorage {

    @Override
    public String put(AvatarImageBO image) {
        throw BusinessException.serviceUnavailable("头像存储未启用");
    }

    @Override
    public String temporaryUrl(String objectKey) {
        return null;
    }

    @Override
    public void deleteQuietly(String objectKey) {
        // 对象存储关闭时没有可清理的远端对象。
    }
}
