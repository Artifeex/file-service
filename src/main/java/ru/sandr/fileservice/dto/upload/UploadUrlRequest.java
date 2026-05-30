package ru.sandr.fileservice.dto.upload;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.sandr.fileservice.dto.upload.context.FileContext;
import ru.sandr.fileservice.dto.upload.jackson.UploadUrlRequestDeserializer;

@JsonDeserialize(using = UploadUrlRequestDeserializer.class)
public record UploadUrlRequest(
        @NotBlank String originalFilename,
        @NotNull FileDomain domain,
        @NotBlank String contentType,
        @Positive long contentLength,
        @Valid FileContext context // Valid - обязателен, чтобы Spring провалидировал внутренности
) {
}
