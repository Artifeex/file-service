package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@JsonTypeName("USER_AVATAR")
public record UserAvatarContext(
        @NotNull UUID userId
) implements FileContext {
}
