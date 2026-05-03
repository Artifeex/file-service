package ru.sandr.fileservice.service.policy;

import org.springframework.stereotype.Component;
import ru.sandr.fileservice.dto.upload.FileDomain;
import ru.sandr.fileservice.enums.UserRole;
import ru.sandr.fileservice.exception.AccessDeniedException;
import ru.sandr.fileservice.service.UserContext;

@Component
public class StudentUploadPolicy implements UploadPolicy {

    @Override
    public boolean supports(UserContext userContext) {
        return userContext.roles().contains(UserRole.ROLE_STUDENT.name());
    }

    @Override
    public void checkPermission(FileDomain fileDomain, UserContext userContext) {
        switch (fileDomain) {
            case COURSE_AVATAR, COURSE_MATERIAL -> throw new AccessDeniedException(
                    "FORBIDDEN",
                    "У пользователя недостаточно прав для выполнения операции"
            );
        }
    }
}
