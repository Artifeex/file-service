package ru.sandr.fileservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import ru.sandr.fileservice.dto.DownloadUrlResponse;
import ru.sandr.fileservice.dto.upload.UploadUrlRequest;
import ru.sandr.fileservice.dto.upload.UploadUrlResponse;
import ru.sandr.fileservice.service.FileService;
import ru.sandr.fileservice.service.UserContext;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload-request")
    public UploadUrlResponse createUploadRequest(
            @Valid @RequestBody UploadUrlRequest uploadUrlRequest,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return fileService.createUploadRequest(uploadUrlRequest, userId, authentication.getAuthorities());
    }

    @GetMapping("/{fileId}")
    public DownloadUrlResponse getDownloadUrl(
            @PathVariable(required = true) UUID fileId,
            Authentication authentication
    ) {
        UserContext userContext = new UserContext(
                UUID.fromString(authentication.getName()), authentication.getAuthorities().stream().map(
                GrantedAuthority::getAuthority).collect(
                Collectors.toSet())
        );
        return fileService.generateDownloadUrl(userContext, fileId);
    }
}
