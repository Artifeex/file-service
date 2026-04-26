package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "domain",
        visible = true
)
public sealed interface FileContext permits CourseAvatarContext, CourseMaterialContext, TaskAnswerContext, UserAvatarContext {
}
