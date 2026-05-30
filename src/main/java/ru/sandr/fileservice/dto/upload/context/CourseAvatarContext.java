package ru.sandr.fileservice.dto.upload.context;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Context for COURSE_AVATAR uploads")
public record CourseAvatarContext(
        @Schema(description = "Target course id", example = "4b4f60a5-7b67-44f3-9e5a-9df45f77f653")
        @NotNull UUID courseId
) implements FileContext {
}
