package ru.sandr.fileservice.file.api.dto;

import java.util.UUID;

public record UploadRequestResponse(
        UUID fileId,
        String uploadUrl
) {
}
