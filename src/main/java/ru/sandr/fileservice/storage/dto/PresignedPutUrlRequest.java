package ru.sandr.fileservice.storage.dto;

import java.time.Duration;

public record PresignedPutUrlRequest(
        String bucketName,
        String s3Key,
        String contentType,
        Duration expiresIn
) {
}
