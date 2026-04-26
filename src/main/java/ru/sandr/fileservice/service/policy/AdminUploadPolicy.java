package ru.sandr.fileservice.service.policy;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.sandr.fileservice.dto.upload.FileDomain;
import ru.sandr.fileservice.exception.ValidationException;
import ru.sandr.fileservice.dto.UploadContext;
import ru.sandr.fileservice.dto.upload.context.FileContext;
import ru.sandr.fileservice.service.UserContext;

@Component
public class AdminUploadPolicy implements UploadPolicy {

    @Override
    public boolean supports(UserContext userContext) {
        return userContext.roles().contains("ROLE_ADMIN");
    }

    @Override
    public void checkPermission(FileDomain fileDomain, UserContext userContext) {
        return; // На текущий момент админу так же можно грузить любые виды файлов
    }
}
