package ru.sandr.fileservice.file.service.policy;

import ru.sandr.fileservice.file.api.dto.UploadRequestRequest;
import ru.sandr.fileservice.file.service.UserContext;

public interface UploadPolicy {

    boolean supports(UserContext userContext);

    void validate(UploadRequestRequest request, UserContext userContext);
}
