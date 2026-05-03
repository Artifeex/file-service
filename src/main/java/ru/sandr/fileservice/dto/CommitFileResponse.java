package ru.sandr.fileservice.dto;

import java.util.UUID;

public record CommitFileResponse(
        UUID fileId,
        String fileUrl
) {
}
