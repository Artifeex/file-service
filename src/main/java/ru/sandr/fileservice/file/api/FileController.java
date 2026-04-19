package ru.sandr.fileservice.file.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.sandr.fileservice.file.api.dto.UploadRequestRequest;
import ru.sandr.fileservice.file.api.dto.UploadRequestResponse;
import ru.sandr.fileservice.file.service.FileService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload-request")
    public UploadRequestResponse createUploadRequest(
            @Valid @RequestBody UploadRequestRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return fileService.createUploadRequest(request, jwt);
    }
}
