package ru.sandr.fileservice.service.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.sandr.fileservice.exception.ObjectNotFoundException;
import ru.sandr.fileservice.service.UserContext;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UploadPolicyFactory {

    private final List<UploadPolicy> uploadPolicies;

    public UploadPolicy resolve(UserContext userContext) {
        return uploadPolicies.stream()
                             .filter(policy -> policy.supports(userContext))
                             .findFirst()
                             .orElseThrow(() -> new ObjectNotFoundException(
                                     "OBJECT_NOT_FOUND",
                                     "По переданному контексту не найдена политика обработки"
                             ));
    }
}
