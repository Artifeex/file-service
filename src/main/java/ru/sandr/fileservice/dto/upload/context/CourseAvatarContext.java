package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@JsonTypeName("COURSE_AVATAR")
public record CourseAvatarContext(
        @NotBlank UUID courseId
) implements FileContext {
}
