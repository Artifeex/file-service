package ru.sandr.fileservice.dto.upload.context;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Context for ANSWER_FILE uploads")
public record TaskAnswerContext(
        @Schema(description = "Course id", example = "4b4f60a5-7b67-44f3-9e5a-9df45f77f653")
        @NotNull UUID courseId,
        @Schema(description = "Student user id", example = "8c1e2f3a-4b5c-6d7e-8f9a-0b1c2d3e4f5a")
        @NotNull UUID userId
) implements FileContext {
}
