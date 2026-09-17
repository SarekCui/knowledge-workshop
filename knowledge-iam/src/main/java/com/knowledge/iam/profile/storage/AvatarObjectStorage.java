package com.knowledge.iam.profile.storage;

import com.knowledge.iam.profile.bo.AvatarImageBO;

public interface AvatarObjectStorage {

    String put(AvatarImageBO image);

    String temporaryUrl(String objectKey);

    void deleteQuietly(String objectKey);
}
