package ru.sandr.fileservice.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends CustomException {

    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
