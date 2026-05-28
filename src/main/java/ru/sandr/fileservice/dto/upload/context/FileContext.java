package ru.sandr.fileservice.dto.upload.context;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "domain"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CourseAvatarContext.class, name = "COURSE_AVATAR"),
        @JsonSubTypes.Type(value = CourseMaterialContext.class, name = "COURSE_MATERIAL"),
        @JsonSubTypes.Type(value = TaskAnswerContext.class, name = "ANSWER_FILE"),
        @JsonSubTypes.Type(value = UserAvatarContext.class, name = "USER_AVATAR")
})
public sealed interface FileContext permits CourseAvatarContext, CourseMaterialContext, TaskAnswerContext, UserAvatarContext {
}
