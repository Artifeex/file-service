package ru.sandr.fileservice.dto.upload;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.sandr.fileservice.dto.upload.context.FileContext;
import ru.sandr.fileservice.dto.upload.jackson.UploadUrlRequestDeserializer;

@Schema(description = "Request payload to obtain a presigned upload URL")
@JsonDeserialize(using = UploadUrlRequestDeserializer.class)
public record UploadUrlRequest(
        @Schema(description = "Original client filename", example = "avatar.png")
        @NotBlank String originalFilename,
        @Schema(description = "Upload domain defining MIME and size limits")
        @NotNull FileDomain domain,
        @Schema(description = "MIME type of the file", example = "image/png")
        @NotBlank String contentType,
        @Schema(description = "File size in bytes", example = "102400")
        @Positive long contentLength,
        @Schema(description = "Domain-specific context (shape depends on domain)")
        @Valid FileContext context
) {
}
