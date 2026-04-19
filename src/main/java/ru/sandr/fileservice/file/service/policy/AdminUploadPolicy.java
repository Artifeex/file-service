package ru.sandr.fileservice.file.service.policy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.sandr.fileservice.common.exception.ValidationException;
import ru.sandr.fileservice.file.api.dto.UploadContext;
import ru.sandr.fileservice.file.api.dto.UploadRequestRequest;
import ru.sandr.fileservice.file.service.UserContext;

@Component
public class AdminUploadPolicy implements UploadPolicy {

    @Override
    public boolean supports(UserContext userContext) {
        return userContext.roles().contains("ADMIN");
    }

    @Override
    public void validate(UploadRequestRequest request, UserContext userContext) {
        validateBaseRequest(request);
        validateContextFields(request);
    }

    private void validateBaseRequest(UploadRequestRequest request) {
        if (!StringUtils.hasText(request.contentType())) {
            throw new ValidationException("contentType must not be blank");
        }
    }

    private void validateContextFields(UploadRequestRequest request) {
        UploadContext context = request.context();
        switch (context) {
            case USER_AVATAR -> {
            }
            case COURSE_COVER -> requireField(request.courseId(), "courseId");
            case COURSE_VIDEO -> {
                requireField(request.courseId(), "courseId");
                requireField(request.lessonId(), "lessonId");
            }
            case HOMEWORK -> {
                requireField(request.assignmentId(), "assignmentId");
                requireField(request.studentId(), "studentId");
            }
        }
    }

    private void requireField(UUID value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " is required for selected upload context");
        }
    }
}
