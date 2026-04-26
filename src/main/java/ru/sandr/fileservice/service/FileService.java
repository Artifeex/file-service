package ru.sandr.fileservice.service;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sandr.fileservice.dto.upload.UploadUrlRequest;
import ru.sandr.fileservice.exception.ObjectNotFoundException;
import ru.sandr.fileservice.exception.ValidationException;
import ru.sandr.fileservice.config.S3Properties;
import ru.sandr.fileservice.dto.CommitFileRequest;
import ru.sandr.fileservice.dto.CommitFileResponse;
import ru.sandr.fileservice.dto.DownloadUrlResponse;
import ru.sandr.fileservice.dto.upload.UploadRequestResponse;
import ru.sandr.fileservice.entity.FileMetadata;
import ru.sandr.fileservice.dao.FileRepository;
import ru.sandr.fileservice.entity.FileStatus;
import ru.sandr.fileservice.mapper.FileMapper;
import ru.sandr.fileservice.service.policy.UploadPolicy;
import ru.sandr.fileservice.service.policy.UploadPolicyFactory;
import ru.sandr.fileservice.storage.S3Service;
import ru.sandr.fileservice.storage.dto.PresignedGetUrlRequest;
import ru.sandr.fileservice.storage.dto.PresignedPutUrlRequest;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final long MAX_FILE_SIZE_BYTES = 300L * 1024 * 1024;
    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofHours(2);

    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final S3Properties s3Properties;
    private final UserContextExtractor userContextExtractor;
    private final UploadPolicyFactory uploadPolicyFactory;
    private final S3KeyBuilder s3KeyBuilder;
    private final FileMapper fileMapper;

    @Transactional
    public UploadRequestResponse createUploadRequest(UploadUrlRequest uploadUrlRequest, UUID userId, Collection<GrantedAuthority> authorities) {
        // Проверить, что текущий пользователь имеет право на загрузку контента, а так же, что пееданный тип является допустимым
        var domain = uploadUrlRequest.domain();
        domain.validateContentType(uploadUrlRequest.contentType());
        // Проверить, что текущий пользователь имеет право на загрузку данного типа контента
        UserContext userContext = new UserContext(userId, authorities.stream().map(GrantedAuthority::getAuthority).collect(
                Collectors.toSet()));
        UploadPolicy uploadPolicy = uploadPolicyFactory.resolve(userContext);
        uploadPolicy.checkPermission(domain, userContext);

        UUID fileId = UUID.randomUUID();

        String s3Key = s3KeyBuilder.build(uploadUrlRequest, fileId);
        String bucketName = s3KeyBuilder.resolveBucketName(
                domain,
                s3Properties.publicBucket(),
                s3Properties.privateBucket()
        );

        FileMetadata fileMetadata = fileMapper.toPendingEntity(fileId, userId, uploadUrlRequest, bucketName, s3Key);
        fileRepository.save(fileMetadata);

        String uploadUrl = s3Service.generatePresignedPutUrl(new PresignedPutUrlRequest(
                bucketName,
                s3Key,
                uploadUrlRequest.contentType(),
                UPLOAD_URL_TTL
        ));

        return new UploadRequestResponse(fileId, uploadUrl);
    }

    @Transactional
    public CommitFileResponse commitFile(CommitFileRequest request) {
        FileMetadata fileMetadata = fileRepository.findById(request.fileId())
                                                  .orElseThrow(() -> new ObjectNotFoundException("File not found: " + request.fileId()));

        if (fileMetadata.getStatus() == FileStatus.PENDING) {
            boolean exists = s3Service.objectExists(fileMetadata.getBucketName(), fileMetadata.getS3Key());
            if (!exists) {
                throw new ValidationException("File object was not uploaded to storage yet");
            }
            fileMetadata.setStatus(FileStatus.ACTIVE);
        }

        String fileUrl = null;
        if (isPublicBucket(fileMetadata.getBucketName())) {
            fileUrl = buildPublicFileUrl(fileMetadata.getBucketName(), fileMetadata.getS3Key());
        }

        return new CommitFileResponse(fileMetadata.getId(), fileUrl);
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponse generateDownloadUrl(UUID fileId) {
        FileMetadata fileMetadata = fileRepository.findById(fileId)
                                                  .orElseThrow(() -> new ObjectNotFoundException("File not found: " + fileId));

        if (fileMetadata.getStatus() != FileStatus.ACTIVE) {
            throw new ValidationException("File is not ACTIVE");
        }

        if (isPublicBucket(fileMetadata.getBucketName())) {
            return new DownloadUrlResponse(buildPublicFileUrl(fileMetadata.getBucketName(), fileMetadata.getS3Key()));
        }

        String downloadUrl = s3Service.generatePresignedGetUrl(new PresignedGetUrlRequest(
                fileMetadata.getBucketName(),
                fileMetadata.getS3Key(),
                DOWNLOAD_URL_TTL
        ));

        return new DownloadUrlResponse(downloadUrl);
    }

    private boolean isPublicBucket(String bucketName) {
        return s3Properties.publicBucket().equals(bucketName);
    }

    private String buildPublicFileUrl(String bucketName, String s3Key) {
        String endpoint = s3Properties.endpoint();
        String normalizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return normalizedEndpoint + "/" + bucketName + "/" + s3Key;
    }
}
