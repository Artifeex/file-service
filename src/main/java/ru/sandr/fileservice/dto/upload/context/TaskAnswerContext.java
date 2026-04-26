package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.UUID;

@JsonTypeName("TASK_ANSWER")
public record TaskAnswerContext(
        UUID courseId,
        UUID userId
) implements FileContext {
}
