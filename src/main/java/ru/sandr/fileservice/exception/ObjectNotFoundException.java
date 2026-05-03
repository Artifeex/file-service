package ru.sandr.fileservice.exception;

import org.springframework.http.HttpStatus;

public class ObjectNotFoundException extends CustomException {

    public ObjectNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
