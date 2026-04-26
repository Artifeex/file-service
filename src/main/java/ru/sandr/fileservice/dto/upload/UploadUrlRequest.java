package ru.sandr.fileservice.dto.upload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import ru.sandr.fileservice.dto.upload.context.FileContext;

public record UploadUrlRequest(
        @NotBlank String originalFilename,
        @NotBlank FileDomain domain,
        @NotBlank String contentType,
        @Positive long contentLength,
        @Valid FileContext context // Valid - обязателен, чтобы Spring провалидировал внутренности
) {
}
