package ru.sandr.fileservice.dto.upload;

import java.util.UUID;

public record UploadUrlResponse(
        UUID fileId,
        String uploadUrl
) {
}
