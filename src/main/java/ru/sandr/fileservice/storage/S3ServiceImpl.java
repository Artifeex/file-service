package ru.sandr.fileservice.storage;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

    private static final Duration MAX_PRESIGN_TTL = Duration.ofDays(7);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Override
    public String generatePresignedPutUrl(PresignedPutUrlRequest request) {
        validateCommonFields(request.bucketName(), request.s3Key());
        requireNonBlank(request.contentType(), "contentType");
        validateDuration(request.expiresIn());

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
        validateCommonFields(request.bucketName(), request.s3Key());
        validateDuration(request.expiresIn());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(request.bucketName())
                .key(request.s3Key())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(request.expiresIn())
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public boolean objectExists(String bucketName, String s3Key) {
        validateCommonFields(bucketName, s3Key);

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
        validateCommonFields(bucketName, s3Key);

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build());
    }

    private void validateCommonFields(String bucketName, String s3Key) {
        requireNonBlank(bucketName, "bucketName");
        requireNonBlank(s3Key, "s3Key");
    }

    private void requireNonBlank(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private void validateDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("expiresIn must be a positive duration");
        }

        if (duration.compareTo(MAX_PRESIGN_TTL) > 0) {
            throw new IllegalArgumentException("expiresIn must be less than or equal to 7 days");
        }
    }
}
