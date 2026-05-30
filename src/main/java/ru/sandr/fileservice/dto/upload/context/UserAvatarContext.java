package ru.sandr.fileservice.dto.upload.context;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Context for USER_AVATAR uploads")
public record UserAvatarContext(
        @Schema(description = "Target user id", example = "4b4f60a5-7b67-44f3-9e5a-9df45f77f653")
        @NotNull UUID userId
) implements FileContext {
}
