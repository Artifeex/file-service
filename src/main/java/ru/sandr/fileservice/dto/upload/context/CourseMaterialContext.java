package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;

@JsonTypeName("COURSE_MATERIAL")
public record CourseMaterialContext(
        String domain,
       @NotNull String courseId
) implements FileContext {
}
