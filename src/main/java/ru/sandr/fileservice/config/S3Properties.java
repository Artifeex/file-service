package ru.sandr.fileservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "s3")
public record S3Properties(
        @NotBlank String endpoint, // Не должен заканчиваться на слэш. Т.е. должен быть в формате localhost:9090
        String presignedEndpoint, // Endpoint, который будет попадать в presigned URL для внешних клиентов
        @NotBlank String region,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String publicBucket,
        @NotBlank String privateBucket
) {

    /**
     * Endpoint, доступный клиентам (браузер, фронтенд). В Docker внутренний endpoint — minio:9000,
     * а presigned/public URL должны указывать на localhost (или внешний хост).
     */
    public String externalEndpoint() {
        if (presignedEndpoint != null && !presignedEndpoint.isBlank()) {
            return presignedEndpoint;
        }
        return endpoint;
    }
}
