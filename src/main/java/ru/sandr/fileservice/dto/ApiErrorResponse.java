package ru.sandr.fileservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class ApiErrorResponse {

    private final String message;
    private final String errorCode;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<ValidationError> violations = new ArrayList<>();

    public void addViolation(String fieldName, String errorMessage) {
        violations.add(new ValidationError(fieldName, errorMessage));
    }

    public record ValidationError(String fieldName, String errorMessage) {}
}
