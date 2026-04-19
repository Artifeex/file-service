package ru.sandr.fileservice.file.service;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.sandr.fileservice.file.api.dto.UploadRequestRequest;
import ru.sandr.fileservice.file.domain.FileEntity;
import ru.sandr.fileservice.file.domain.FileStatus;

@Component
public class FileMapper {

    public FileEntity toPendingEntity(
            UUID fileId,
            UserContext userContext,
            UploadRequestRequest request,
            String bucketName,
            String s3Key
    ) {
        return FileEntity.builder()
                .id(fileId)
                .ownerId(userContext.userId())
                .originalFilename(request.filename())
                .bucketName(bucketName)
                .s3Key(s3Key)
                .sizeBytes(request.sizeBytes())
                .contentType(request.contentType())
                .status(FileStatus.PENDING)
                .build();
    }
}
