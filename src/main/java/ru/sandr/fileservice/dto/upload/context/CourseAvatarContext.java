package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@JsonTypeName("COURSE_AVATAR")
public record CourseAvatarContext(
        String domain,
        @NotNull UUID courseId
) implements FileContext {
}
