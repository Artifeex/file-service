package ru.sandr.fileservice.file.service;

import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.sandr.fileservice.common.exception.ValidationException;
import ru.sandr.fileservice.file.api.dto.UploadContext;
import ru.sandr.fileservice.file.api.dto.UploadRequestRequest;

@Component
public class S3KeyBuilder {

    public String build(UploadRequestRequest request, UserContext userContext, UUID generatedFileName) {
        String extension = extractExtension(request.filename());

        return switch (request.context()) {
            case USER_AVATAR -> "avatars/%s/%s.%s".formatted(userContext.userId(), generatedFileName, extension);
            case COURSE_COVER -> "course-covers/%s/%s.%s".formatted(request.courseId(), generatedFileName, extension);
            case COURSE_VIDEO -> "courses/%s/lessons/%s/%s.%s".formatted(
                    request.courseId(),
                    request.lessonId(),
                    generatedFileName,
                    extension
            );
            case HOMEWORK -> "homeworks/%s/students/%s/%s.%s".formatted(
                    request.assignmentId(),
                    request.studentId(),
                    generatedFileName,
                    extension
            );
        };
    }

    public String resolveBucketName(UploadContext context, String publicBucket, String privateBucket) {
        return switch (context) {
            case USER_AVATAR, COURSE_COVER -> publicBucket;
            case COURSE_VIDEO, HOMEWORK -> privateBucket;
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
