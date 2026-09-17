package com.knowledge.storage.minio;

import com.knowledge.storage.core.ObjectStorage;
import com.knowledge.storage.exception.ObjectStorageException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.time.Duration;

public class MinioObjectStorage implements ObjectStorage {

    private final MinioClient minioClient;
    private final String bucket;

    private MinioObjectStorage(MinioClient minioClient, String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public static ObjectStorage create(String endpoint, String accessKey, String secretKey, String bucket) {
        MinioClient client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            return new MinioObjectStorage(client, bucket);
        } catch (Exception exception) {
            throw new ObjectStorageException("对象存储初始化失败", exception);
        }
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(objectKey)
                    .contentType(contentType).stream(stream, (long) content.length, -1L).build());
        } catch (Exception exception) {
            throw new ObjectStorageException("对象上传失败", exception);
        }
    }

    @Override
    public String presignedGetUrl(String objectKey, Duration validity) {
        if (objectKey == null || objectKey.isBlank()) return null;
        long seconds = validity.toSeconds();
        if (seconds < 1 || seconds > 604800) throw new IllegalArgumentException("签名有效期必须在1秒到7天之间");
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Http.Method.GET).bucket(bucket).object(objectKey).expiry((int) seconds).build());
        } catch (Exception exception) {
            throw new ObjectStorageException("对象访问地址生成失败", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) {
            throw new ObjectStorageException("对象删除失败", exception);
        }
    }
}
