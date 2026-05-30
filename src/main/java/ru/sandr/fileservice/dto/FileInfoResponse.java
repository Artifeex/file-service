package ru.sandr.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "File metadata exposed to API clients")
public record FileInfoResponse(
        @Schema(description = "File identifier", example = "4b4f60a5-7b67-44f3-9e5a-9df45f77f653")
        UUID fileId,
        @Schema(description = "Original filename", example = "lecture-01.mp4")
        String fileName,
        @Schema(description = "MIME type", example = "video/mp4")
        String mimeType,
        @Schema(description = "Lifecycle status", example = "ACTIVE", allowableValues = {"PENDING", "ACTIVE"})
        String status
) {
}
