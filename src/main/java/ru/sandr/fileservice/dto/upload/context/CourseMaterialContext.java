package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@JsonTypeName("COURSE_MATERIAL")
public record CourseMaterialContext(
        @NotNull UUID courseId
) implements FileContext {
}
