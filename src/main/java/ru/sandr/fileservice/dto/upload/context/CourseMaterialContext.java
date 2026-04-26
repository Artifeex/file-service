package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("COURSE_MATERIAL")
public record CourseMaterialContext(
        String courseId
) implements FileContext {
}
