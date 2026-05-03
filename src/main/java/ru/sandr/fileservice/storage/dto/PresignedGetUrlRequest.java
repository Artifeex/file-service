package ru.sandr.fileservice.storage.dto;

import ru.sandr.fileservice.entity.FileMetadata;

import java.time.Duration;

public record PresignedGetUrlRequest(
        FileMetadata fileMetadata,
        Duration expiresIn
) {
}
