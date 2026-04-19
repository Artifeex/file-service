package ru.sandr.fileservice.file.service;

import org.springframework.security.oauth2.jwt.Jwt;
import ru.sandr.fileservice.file.api.dto.UploadRequestRequest;
import ru.sandr.fileservice.file.api.dto.UploadRequestResponse;

public interface FileService {

    UploadRequestResponse createUploadRequest(UploadRequestRequest request, Jwt jwt);
}
