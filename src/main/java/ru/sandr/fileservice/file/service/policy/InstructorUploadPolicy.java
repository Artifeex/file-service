package ru.sandr.fileservice.file.service.policy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.sandr.fileservice.common.exception.ForbiddenOperationException;
import ru.sandr.fileservice.common.exception.ValidationException;
import ru.sandr.fileservice.file.api.dto.UploadContext;
import ru.sandr.fileservice.file.api.dto.UploadRequestRequest;
import ru.sandr.fileservice.file.service.UserContext;

@Component
public class InstructorUploadPolicy implements UploadPolicy {

    @Override
    public boolean supports(UserContext userContext) {
        return userContext.roles().contains("INSTRUCTOR") || userContext.roles().contains("TEACHER");
    }

    @Override
    public void validate(UploadRequestRequest request, UserContext userContext) {
        if (request.context() == UploadContext.HOMEWORK) {
            throw new ForbiddenOperationException("Instructors are not allowed to upload HOMEWORK content");
        }

        switch (request.context()) {
            case USER_AVATAR -> {
            }
            case COURSE_COVER -> requireField(request.courseId(), "courseId");
            case COURSE_VIDEO -> {
                requireField(request.courseId(), "courseId");
                requireField(request.lessonId(), "lessonId");
            }
            case HOMEWORK -> {
            }
        }
    }

    private void requireField(UUID value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " is required for selected upload context");
        }
    }
}
