package ru.sandr.fileservice.dto.upload;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Presigned upload URL and created file identifier")
public record UploadUrlResponse(
        @Schema(description = "Created file record id", example = "4b4f60a5-7b67-44f3-9e5a-9df45f77f653")
        UUID fileId,
        @Schema(description = "Presigned PUT URL valid for 15 minutes")
        String uploadUrl
) {
}
