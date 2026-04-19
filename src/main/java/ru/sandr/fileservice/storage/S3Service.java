package ru.sandr.fileservice.storage;

import ru.sandr.fileservice.storage.dto.PresignedGetUrlRequest;
import ru.sandr.fileservice.storage.dto.PresignedPutUrlRequest;

public interface S3Service {

    String generatePresignedPutUrl(PresignedPutUrlRequest request);

    String generatePresignedGetUrl(PresignedGetUrlRequest request);

    boolean objectExists(String bucketName, String s3Key);

    void deleteObject(String bucketName, String s3Key);
}
