package ru.sandr.fileservice.file.service.policy;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sandr.fileservice.common.exception.ForbiddenOperationException;
import ru.sandr.fileservice.file.service.UserContext;

@Component
@RequiredArgsConstructor
public class UploadPolicyFactory {

    private final List<UploadPolicy> uploadPolicies;

    public UploadPolicy resolve(UserContext userContext) {
        return uploadPolicies.stream()
                .filter(policy -> policy.supports(userContext))
                .findFirst()
                .orElseThrow(() -> new ForbiddenOperationException("No upload policy found for authenticated role"));
    }
}
