package ru.sandr.fileservice.storage;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import ru.sandr.fileservice.storage.dto.PresignedGetUrlRequest;
import ru.sandr.fileservice.storage.dto.PresignedPutUrlRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    // Белый список MIME-типов, которые безопасно открывать в браузере
    private static final Set<String> SAFE_INLINE_TYPES = Set.of(
            "application/pdf",
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "video/mp4", "video/webm",
            "audio/mpeg", "audio/ogg"
    );

    @Override
    public String generatePresignedPutUrl(PresignedPutUrlRequest request) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(request.bucketName())
                                                            .key(request.s3Key())
                                                            .contentType(request.contentType())
                                                            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                                                                        .signatureDuration(request.expiresIn())
                                                                        .putObjectRequest(putObjectRequest)
                                                                        .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    @Override
    public String generatePresignedGetUrl(PresignedGetUrlRequest request) {
        var fileMetadata = request.fileMetadata();
        // Для кодирования русских символов, которые не поддерживаеются браузерами в заголовках.
        // Но потом, т.к. мы передаем браузеру информацию о том, что мы закодировали имя файла - то он его
        // Для отображения уже раскодирует, но при этом при передаче от s3 в браузер имя файла будет закодировано
        String encodedFilename = UriUtils.encode(fileMetadata.getFileName(), StandardCharsets.UTF_8);
        String nameParameters = "filename*=UTF-8''" + encodedFilename;
        String disposition;
        if (SAFE_INLINE_TYPES.contains(fileMetadata.getContentType())) {
            disposition = "inline; " + nameParameters;
        } else {
            disposition = "attachment; " + nameParameters;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(fileMetadata.getBucketName())
                                                            .key(fileMetadata.getS3Key())
                                                            .responseContentType(fileMetadata.getContentType())
                                                            .responseContentDisposition(disposition)
                                                            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                                                                        .signatureDuration(request.expiresIn())
                                                                        .getObjectRequest(getObjectRequest)
                                                                        .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public boolean objectExists(String bucketName, String s3Key) {

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                                                 .bucket(bucketName)
                                                 .key(s3Key)
                                                 .build());
            return true;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    @Override
    public void deleteObject(String bucketName, String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                                                 .bucket(bucketName)
                                                 .key(s3Key)
                                                 .build());
    }
}
