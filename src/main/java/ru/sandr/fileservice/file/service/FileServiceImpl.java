package ru.sandr.fileservice.file.service;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sandr.fileservice.common.exception.ValidationException;
import ru.sandr.fileservice.config.S3Properties;
import ru.sandr.fileservice.file.api.dto.UploadRequestRequest;
import ru.sandr.fileservice.file.api.dto.UploadRequestResponse;
import ru.sandr.fileservice.file.domain.FileEntity;
import ru.sandr.fileservice.file.domain.FileRepository;
import ru.sandr.fileservice.file.service.policy.UploadPolicy;
import ru.sandr.fileservice.file.service.policy.UploadPolicyFactory;
import ru.sandr.fileservice.storage.S3Service;
import ru.sandr.fileservice.storage.dto.PresignedPutUrlRequest;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long MAX_FILE_SIZE_BYTES = 300L * 1024 * 1024;
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);

    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final S3Properties s3Properties;
    private final UserContextExtractor userContextExtractor;
    private final UploadPolicyFactory uploadPolicyFactory;
    private final S3KeyBuilder s3KeyBuilder;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public UploadRequestResponse createUploadRequest(UploadRequestRequest request, Jwt jwt) {
        if (request.sizeBytes() > MAX_FILE_SIZE_BYTES) {
            throw new ValidationException("File size exceeds the maximum allowed limit of 300MB");
        }

        UserContext userContext = userContextExtractor.extract(jwt);
        UploadPolicy uploadPolicy = uploadPolicyFactory.resolve(userContext);
        uploadPolicy.validate(request, userContext);

        UUID fileId = UUID.randomUUID();
        String s3Key = s3KeyBuilder.build(request, userContext, fileId);
        String bucketName = s3KeyBuilder.resolveBucketName(
                request.context(),
                s3Properties.publicBucket(),
                s3Properties.privateBucket()
        );

        FileEntity fileEntity = fileMapper.toPendingEntity(fileId, userContext, request, bucketName, s3Key);
        fileRepository.save(fileEntity);

        String uploadUrl = s3Service.generatePresignedPutUrl(new PresignedPutUrlRequest(
                bucketName,
                s3Key,
                request.contentType(),
                UPLOAD_URL_TTL
        ));

        return new UploadRequestResponse(fileId, uploadUrl);
    }
}
