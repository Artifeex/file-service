package ru.sandr.fileservice.service;

import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.sandr.fileservice.dto.upload.FileDomain;
import ru.sandr.fileservice.dto.upload.UploadUrlRequest;
import ru.sandr.fileservice.dto.upload.context.CourseAvatarContext;
import ru.sandr.fileservice.dto.upload.context.CourseMaterialContext;
import ru.sandr.fileservice.dto.upload.context.TaskAnswerContext;
import ru.sandr.fileservice.dto.upload.context.UserAvatarContext;
import ru.sandr.fileservice.exception.ValidationException;

@Component
public class S3KeyBuilder {

    private static final String USER_AVATAR_S3_KEY_PATTERN = "users/%s/avatars/%s";
    private static final String COURSE_AVATAR_S3_KEY_PATTERN = "courses/%s/avatars/%s";
    private static final String COURSE_MATERIAL_S3_KEY_PATTERN = "courses/%s/materials/%s";
    private static final String TASK_ANSWER_S3_KEY_PATTERN = "courses/%s/answers/%s/%s";

    public String build(UploadUrlRequest uploadUrlRequest, UUID fileId) {
        String extension = extractExtension(uploadUrlRequest.originalFilename());
        String s3FileName = fileId + "." + extension;
        return switch (uploadUrlRequest.domain()) {
            case USER_AVATAR -> {
                var userAvatarContext = (UserAvatarContext) uploadUrlRequest.context();
                yield USER_AVATAR_S3_KEY_PATTERN.formatted(userAvatarContext.userId(), s3FileName);
            }
            case COURSE_AVATAR -> {
                var courseAvatarContext = (CourseAvatarContext) uploadUrlRequest.context();
                yield COURSE_AVATAR_S3_KEY_PATTERN.formatted(courseAvatarContext.courseId(), s3FileName);
            }
            case COURSE_MATERIAL -> {
                var courseMaterialContext = (CourseMaterialContext) uploadUrlRequest.context();
                yield COURSE_MATERIAL_S3_KEY_PATTERN.formatted(courseMaterialContext.courseId(), s3FileName);
            }
            case ANSWER_FILE -> {
                var answerFileContext = (TaskAnswerContext) uploadUrlRequest.context();
                yield TASK_ANSWER_S3_KEY_PATTERN.formatted(answerFileContext.courseId(), answerFileContext.userId(), s3FileName);
            }
        };
    }

    public String resolveBucketName(FileDomain fileDomain, String publicBucket, String privateBucket) {
        return switch (fileDomain) {
            case USER_AVATAR, COURSE_AVATAR -> publicBucket;
            case COURSE_MATERIAL, ANSWER_FILE -> privateBucket;
        };
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new ValidationException("filename must not be blank");
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new ValidationException("filename must include a valid extension");
        }

        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!extension.matches("[a-z0-9]{1,10}")) {
            throw new ValidationException("filename extension contains unsupported characters");
        }
        return extension;
    }
}
