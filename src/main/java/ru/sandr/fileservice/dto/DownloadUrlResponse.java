package ru.sandr.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Presigned download URL for private content")
public record DownloadUrlResponse(
        @Schema(description = "Presigned GET URL valid for 2 hours")
        String downloadUrl
) {
}
