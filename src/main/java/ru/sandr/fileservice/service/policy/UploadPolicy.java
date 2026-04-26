package ru.sandr.fileservice.service.policy;

import ru.sandr.fileservice.dto.upload.FileDomain;
import ru.sandr.fileservice.service.UserContext;

public interface UploadPolicy {

    boolean supports(UserContext userContext);

    void checkPermission(FileDomain fileDomain, UserContext userContext);
}
