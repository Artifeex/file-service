package ru.sandr.fileservice.service.policy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.sandr.fileservice.dto.upload.FileDomain;
import ru.sandr.fileservice.exception.ForbiddenOperationException;
import ru.sandr.fileservice.exception.ValidationException;
import ru.sandr.fileservice.dto.UploadContext;
import ru.sandr.fileservice.dto.upload.context.FileContext;
import ru.sandr.fileservice.service.UserContext;
import software.amazon.awssdk.services.s3.model.AccessDeniedException;

@Component
public class StudentUploadPolicy implements UploadPolicy {

    @Override
    public boolean supports(UserContext userContext) {
        return userContext.roles().contains("ROLE_STUDENT");
    }

    @Override
    public void checkPermission(FileDomain fileDomain, UserContext userContext) {
        switch (fileDomain) {
            case COURSE_AVATAR, COURSE_MATERIAL -> throw AccessDeniedException.create("ACCESS_DENIED", null);
        }
    }

    private void requireField(UUID value, String fieldName) {
        if (value == null) {
            throw new ValidationException(fieldName + " is required for selected upload context");
        }
    }
}
