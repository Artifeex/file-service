package ru.sandr.fileservice.dto.upload.context;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TaskAnswerContext(
        @NotNull UUID courseId,
        @NotNull UUID userId
) implements FileContext {
}
