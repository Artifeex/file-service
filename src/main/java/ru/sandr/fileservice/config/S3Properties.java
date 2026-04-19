package ru.sandr.fileservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        @NotBlank String endpoint,
        @NotBlank String region,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String publicBucket,
        @NotBlank String privateBucket
) {
}
