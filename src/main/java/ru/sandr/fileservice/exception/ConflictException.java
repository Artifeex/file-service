package ru.sandr.fileservice.exception;

public class ConflictException extends CustomException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
