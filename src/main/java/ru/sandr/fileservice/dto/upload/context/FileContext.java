package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "domain",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CourseAvatarContext.class, name = "COURSE_AVATAR"),
        @JsonSubTypes.Type(value = CourseMaterialContext.class, name = "COURSE_MATERIAL"),
        @JsonSubTypes.Type(value = TaskAnswerContext.class, name = "TASK_ANSWER"),
        @JsonSubTypes.Type(value = UserAvatarContext.class, name = "USER_AVATAR")
})
public sealed interface FileContext permits CourseAvatarContext, CourseMaterialContext, TaskAnswerContext, UserAvatarContext {
    String domain();
}
