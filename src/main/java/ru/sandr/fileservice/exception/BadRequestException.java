package ru.sandr.fileservice.exception;

public class BadRequestException extends CustomException {

    public BadRequestException(String code, String message) {
        super(code, message);
    }
}
