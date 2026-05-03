package ru.sandr.fileservice.service.policy;

import org.springframework.stereotype.Component;
import ru.sandr.fileservice.dto.upload.FileDomain;
import ru.sandr.fileservice.enums.UserRole;
import ru.sandr.fileservice.service.UserContext;

@Component
public class TeacherUploadPolicy implements UploadPolicy {

    @Override
    public boolean supports(UserContext userContext) {
        return userContext.roles().contains(UserRole.ROLE_TEACHER.name());
    }

    @Override
    public void checkPermission(FileDomain fileDomain, UserContext userContext) {
        return; // На текущий момент доп проверок не требуется
    }
}
