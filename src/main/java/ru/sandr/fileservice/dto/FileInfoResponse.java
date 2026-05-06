package ru.sandr.fileservice.dto;

import java.util.UUID;

public record FileInfoResponse(
        UUID fileId,
        String fileName,
        String mimeType,
        String status
) {
}
