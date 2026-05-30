package ru.sandr.fileservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
@Schema(description = "Standard error response returned by GlobalExceptionHandler")
public class ApiErrorResponse {

    @Schema(description = "Human-readable error message", example = "File not found")
    private final String message;

    @Schema(description = "Machine-readable error code", example = "OBJECT_NOT_FOUND")
    private final String errorCode;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(description = "Field-level validation errors")
    private final List<ValidationError> violations = new ArrayList<>();

    public void addViolation(String fieldName, String errorMessage) {
        violations.add(new ValidationError(fieldName, errorMessage));
    }

    @Schema(description = "Single field validation error")
    public record ValidationError(
            @Schema(description = "Request field name", example = "contentType")
            String fieldName,
            @Schema(description = "Validation message", example = "must not be blank")
            String errorMessage
    ) {}
}
