package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@JsonTypeName("ANSWER_FILE")
public record TaskAnswerContext(
        @NotNull UUID courseId,
        @NotNull UUID userId
) implements FileContext {
}
