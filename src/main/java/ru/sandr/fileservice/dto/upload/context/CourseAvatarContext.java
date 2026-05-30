package ru.sandr.fileservice.dto.upload.context;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CourseAvatarContext(
        @NotNull UUID courseId
) implements FileContext {
}
