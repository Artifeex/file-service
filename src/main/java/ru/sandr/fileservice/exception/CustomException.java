package ru.sandr.fileservice.exception;

import org.springframework.http.HttpStatus;

public abstract class CustomException extends RuntimeException {

    private final HttpStatus status;

    protected CustomException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
