package ru.sandr.fileservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI fileServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("File Service API")
                        .version("v1")
                        .description("""
                                Contract for presigned upload/download URLs, file metadata and Kafka-driven lifecycle events.
                                File bytes are transferred directly between clients and S3 (MinIO); this service only issues URLs and tracks metadata.
                                """)
                        .contact(new Contact()
                                .name("File Service Team"))
                        .license(new License()
                                .name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME))
                .schemaRequirement(BEARER_AUTH_SCHEME, new SecurityScheme()
                        .name(BEARER_AUTH_SCHEME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT access token issued by Users Service"))
                .tags(List.of(
                        new Tag().name("Files").description("Upload requests, download URLs and file metadata")
                ));
    }
}
