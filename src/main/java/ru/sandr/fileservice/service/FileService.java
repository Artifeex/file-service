package ru.sandr.fileservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sandr.fileservice.config.S3Properties;
import ru.sandr.fileservice.dao.FileRepository;
import ru.sandr.fileservice.dto.DownloadUrlResponse;
import ru.sandr.fileservice.dto.FileInfoResponse;
import ru.sandr.fileservice.dto.upload.UploadUrlRequest;
import ru.sandr.fileservice.dto.upload.UploadUrlResponse;
import ru.sandr.fileservice.entity.FileMetadata;
import ru.sandr.fileservice.entity.FileStatus;
import ru.sandr.fileservice.enums.UserRole;
import ru.sandr.fileservice.exception.AccessDeniedException;
import ru.sandr.fileservice.exception.BadRequestException;
import ru.sandr.fileservice.exception.ObjectNotFoundException;
import ru.sandr.fileservice.mapper.FileMapper;
import ru.sandr.fileservice.service.policy.UploadPolicy;
import ru.sandr.fileservice.service.policy.UploadPolicyFactory;
import ru.sandr.fileservice.storage.S3Service;
import ru.sandr.fileservice.storage.dto.PresignedGetUrlRequest;
import ru.sandr.fileservice.storage.dto.PresignedPutUrlRequest;

import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);
    private static final Duration DOWNLOAD_URL_TTL = Duration.ofHours(2);

    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final S3Properties s3Properties;
    private final UploadPolicyFactory uploadPolicyFactory;
    private final S3KeyBuilder s3KeyBuilder;
    private final FileMapper fileMapper;

    @Transactional
    public UploadUrlResponse createUploadRequest(
            UploadUrlRequest uploadUrlRequest,
            UUID userId,
            Collection<? extends GrantedAuthority> authorities
    ) {
        // Проверить, что текущий пользователь имеет право на загрузку контента, а так же, что пееданный тип является допустимым
        var domain = uploadUrlRequest.domain();
        domain.validateContentType(uploadUrlRequest.contentType());
        // Проверить, что текущий пользователь имеет право на загрузку данного типа контента
        UserContext userContext = new UserContext(
                userId, authorities.stream().map(GrantedAuthority::getAuthority).collect(
                Collectors.toSet())
        );
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

        return new UploadUrlResponse(fileId, uploadUrl);
    }

    @Transactional
    public void commitFile(UUID fileId) {
        FileMetadata fileMetadata = fileRepository.findById(fileId)
                                                  .orElseThrow(() -> new ObjectNotFoundException("OBJECT_NOT_FOUND", "File not found: " + fileId));

        if (fileMetadata.getStatus() == FileStatus.PENDING) {
            boolean exists = s3Service.objectExists(fileMetadata.getBucketName(), fileMetadata.getS3Key());
            if (!exists) {
                throw new ObjectNotFoundException("OBJECT_NOT_FOUND", "File object was not uploaded to storage yet");
            }
            fileMetadata.setStatus(FileStatus.ACTIVE);
        }
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponse generateDownloadUrl(UserContext userContext, UUID fileId) {
        FileMetadata fileMetadata = fileRepository.findById(fileId)
                                                  .orElseThrow(() -> new ObjectNotFoundException("OBJECT_NOT_FOUND", "File not found: " + fileId));

        if (fileMetadata.getStatus() != FileStatus.ACTIVE) {
            throw new BadRequestException("FILE_IS_NOT_ACTIVE", "File is not ACTIVE");
        }

        if (isPublicBucket(fileMetadata.getBucketName())) {
            return new DownloadUrlResponse(buildPublicFileUrl(fileMetadata.getBucketName(), fileMetadata.getS3Key()));
        }
        if (!isHavePermission(userContext, fileMetadata)) {
            throw new AccessDeniedException("ACCESS_DENIED", "User is not have permission to download file");
        }

        String downloadUrl = s3Service.generatePresignedGetUrl(new PresignedGetUrlRequest(
                fileMetadata,
                DOWNLOAD_URL_TTL
        ));

        return new DownloadUrlResponse(downloadUrl);
    }

    private boolean isHavePermission(UserContext userContext, FileMetadata fileMetadata) {
        if (CollectionUtils.isEmpty(userContext.roles())) {
            return false;
        }
        if (userContext.roles().contains(UserRole.ROLE_ADMIN.name()) || userContext.roles()
                                                                                   .contains(UserRole.ROLE_TEACHER.name())) {
            return true;
        }
        return userContext.roles().contains(UserRole.ROLE_STUDENT.name()) && fileMetadata.getOwnerId()
                                                                                         .equals(userContext.userId());
    }

    private boolean isPublicBucket(String bucketName) {
        return s3Properties.publicBucket().equals(bucketName);
    }

    private String buildPublicFileUrl(String bucketName, String s3Key) {
        return s3Properties.endpoint() + "/" + bucketName + "/" + s3Key;
    }

    public void deleteFile(UUID fileId) {
        var fileMetadata = fileRepository.findById(fileId)
                                         .orElseThrow(() -> new ObjectNotFoundException("OBJECT_NOT_FOUND", "File not found: " + fileId));
        fileRepository.delete(fileMetadata);
        var isFileExists = s3Service.objectExists(fileMetadata.getBucketName(), fileMetadata.getS3Key());
        if (isFileExists) {
            s3Service.deleteObject(fileMetadata.getBucketName(), fileMetadata.getS3Key());
        }
    }

    public FileInfoResponse getFileInfo(UUID fileId) {
        var fileMetadata = fileRepository.findById(fileId)
                                         .orElseThrow(() -> new ObjectNotFoundException("OBJECT_NOT_FOUND", "File not found: " + fileId));
        return new FileInfoResponse(
                fileMetadata.getId(),
                fileMetadata.getFileName(),
                fileMetadata.getContentType(),
                fileMetadata.getStatus().name()
        );
    }
}
