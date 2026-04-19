package ru.sandr.fileservice.storage.dto;

import java.time.Duration;

public record PresignedGetUrlRequest(
        String bucketName,
        String s3Key,
        Duration expiresIn
) {
}
