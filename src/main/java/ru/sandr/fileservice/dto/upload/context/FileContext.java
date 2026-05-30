package ru.sandr.fileservice.dto.upload.context;

public sealed interface FileContext permits CourseAvatarContext, CourseMaterialContext, TaskAnswerContext, UserAvatarContext {
}
