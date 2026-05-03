package ru.sandr.fileservice.exception;

public class AccessDeniedException extends CustomException {

    public AccessDeniedException(String code, String message) {
        super(code, message);
    }
}
