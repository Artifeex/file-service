package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@JsonTypeName("USER_AVATAR")
public record UserAvatarContext(
        @NotBlank UUID userId
) implements FileContext {
}
