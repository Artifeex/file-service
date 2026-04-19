package ru.sandr.fileservice.file.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record UploadRequestRequest(
        @NotBlank String filename,
        @NotBlank String contentType,
        @NotNull @Positive Long sizeBytes,
        @NotNull UploadContext context,
        UUID courseId,
        UUID lessonId,
        UUID assignmentId,
        UUID studentId
) {
}
