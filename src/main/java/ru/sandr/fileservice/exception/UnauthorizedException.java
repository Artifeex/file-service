package ru.sandr.fileservice.exception;

public class UnauthorizedException extends CustomException {
    public UnauthorizedException(String code, String message) {
        super(code, message);
    }
}

