package ru.sandr.fileservice.file.service.policy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.sandr.fileservice.common.exception.ForbiddenOperationException;
import ru.sandr.fileservice.common.exception.ValidationException;
import ru.sandr.fileservice.file.api.dto.UploadContext;
import ru.sandr.fileservice.file.api.dto.UploadRequestRequest;
import ru.sandr.fileservice.file.service.UserContext;

@Component
public class StudentUploadPolicy implements UploadPolicy {

    @Override
    public boolean supports(UserContext userContext) {
        return userContext.roles().contains("STUDENT");
    }

    @Override
    public void validate(UploadRequestRequest request, UserContext userContext) {
        if (request.context() == UploadContext.COURSE_VIDEO || request.context() == UploadContext.COURSE_COVER) {
            throw new ForbiddenOperationException("Students are not allowed to upload course content");
        }

        switch (request.context()) {
            case USER_AVATAR -> {
            }
            case HOMEWORK -> {
                requireField(request.assignmentId(), "assignmentId");
                if (request.studentId() == null) {
                    throw new ValidationException("studentId is required for HOMEWORK context");
                }
                if (!request.studentId().equals(userContext.userId())) {
                    throw new ForbiddenOperationException("studentId must match authenticated user for HOMEWORK uploads");
                }
            }
            case COURSE_COVER, COURSE_VIDEO -> {
            }
        }
    }

    private void requireField(UUID value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " is required for selected upload context");
        }
    }
}
