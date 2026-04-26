package ru.sandr.fileservice.mapper;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.sandr.fileservice.dto.upload.UploadUrlRequest;
import ru.sandr.fileservice.dto.upload.context.FileContext;
import ru.sandr.fileservice.entity.FileMetadata;
import ru.sandr.fileservice.entity.FileStatus;
import ru.sandr.fileservice.service.UserContext;

@Component
public class FileMapper {

    public FileMetadata toPendingEntity(
            UUID fileId,
            UUID ownerId,
            UploadUrlRequest uploadUrlRequest,
            String bucketName,
            String s3Key
    ) {
        return FileMetadata.builder()
                           .id(fileId)
                           .ownerId(ownerId)
                           .originalFilename(uploadUrlRequest.originalFilename())
                           .bucketName(bucketName)
                           .s3Key(s3Key)
                           .sizeBytes(uploadUrlRequest.contentLength())
                           .contentType(uploadUrlRequest.contentType())
                           .status(FileStatus.PENDING)
                           .build();
    }
}
