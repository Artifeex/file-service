package ru.sandr.fileservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import ru.sandr.fileservice.dto.ApiErrorResponse;
import ru.sandr.fileservice.dto.DownloadUrlResponse;
import ru.sandr.fileservice.dto.FileInfoResponse;
import ru.sandr.fileservice.dto.upload.UploadUrlRequest;
import ru.sandr.fileservice.dto.upload.UploadUrlResponse;
import ru.sandr.fileservice.service.FileService;
import ru.sandr.fileservice.service.UserContext;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
@Tag(name = "Files")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload-request")
    @Operation(
            summary = "Request presigned upload URL",
            description = """
                    Validates role, domain quotas and content constraints, persists a PENDING file record
                    and returns a presigned PUT URL (15 minutes) for direct upload to S3/MinIO.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload URL issued"),
            @ApiResponse(responseCode = "400", description = "Validation or policy error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public UploadUrlResponse createUploadRequest(
            @Valid @RequestBody UploadUrlRequest uploadUrlRequest,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return fileService.createUploadRequest(uploadUrlRequest, userId, authentication.getAuthorities());
    }

    @GetMapping("/download-url/{fileId}")
    @Operation(
            summary = "Get presigned download URL",
            description = "Returns a presigned GET URL (2 hours) for private content when the caller is authorized."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Download URL issued"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "File not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public DownloadUrlResponse getDownloadUrl(
            @Parameter(description = "File identifier", required = true)
            @PathVariable UUID fileId,
            Authentication authentication
    ) {
        UserContext userContext = new UserContext(
                UUID.fromString(authentication.getName()), authentication.getAuthorities().stream().map(
                GrantedAuthority::getAuthority).collect(
                Collectors.toSet())
        );
        return fileService.generateDownloadUrl(userContext, fileId);
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "Get file metadata", description = "Returns metadata for an ACTIVE or PENDING file record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File metadata"),
            @ApiResponse(responseCode = "404", description = "File not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public FileInfoResponse getFileInfo(
            @Parameter(description = "File identifier", required = true)
            @PathVariable UUID fileId
    ) {
        return fileService.getFileInfo(fileId);
    }
}
