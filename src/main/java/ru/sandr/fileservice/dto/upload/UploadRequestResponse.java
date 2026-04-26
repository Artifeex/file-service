package ru.sandr.fileservice.dto.upload;

import java.util.UUID;

public record UploadRequestResponse(
        UUID fileId,
        String uploadUrl
) {
}
