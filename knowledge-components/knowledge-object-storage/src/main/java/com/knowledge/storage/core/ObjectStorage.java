package com.knowledge.storage.core;

import java.time.Duration;

public interface ObjectStorage {

    void put(String objectKey, byte[] content, String contentType);

    String presignedGetUrl(String objectKey, Duration validity);

    void delete(String objectKey);
}
