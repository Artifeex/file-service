package ru.sandr.fileservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.sandr.fileservice.dto.upload.UploadUrlRequest;
import ru.sandr.fileservice.dto.upload.context.FileContext;
import ru.sandr.fileservice.dto.upload.UploadRequestResponse;
import ru.sandr.fileservice.service.FileService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload-request")
    public UploadRequestResponse createUploadRequest(
            @Valid @RequestBody UploadUrlRequest uploadUrlRequest,
            @AuthenticationPrincipal JwtAuthenticationToken jwt
    ) {
        UUID userId = UUID.fromString(jwt.getName());
        return fileService.createUploadRequest(uploadUrlRequest, userId, jwt.getAuthorities());
    }
}
