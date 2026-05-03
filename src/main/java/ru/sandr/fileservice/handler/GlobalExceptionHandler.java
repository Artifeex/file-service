package ru.sandr.fileservice.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.sandr.fileservice.dto.ApiErrorResponse;
import ru.sandr.fileservice.exception.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    //

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleUnauthorized(UnauthorizedException ex) {
        return new ApiErrorResponse(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleBadCredentials(BadCredentialsException ex) {
        // Возвращаем абстрактное сообщение в целях безопасности (не уточняем, логин неверный или пароль)
        return new ApiErrorResponse("Неверный логин или пароль", "BAD_CREDENTIALS");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return new ApiErrorResponse(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(ObjectNotFoundException ex) {
        return new ApiErrorResponse(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleConflict(ConflictException ex) {
        return new ApiErrorResponse(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        var response = new ApiErrorResponse("Ошибка валидации данных", "VALIDATION_FAILED");
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String filedName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            response.addViolation(filedName, errorMessage);
        });

        return response;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Ошибка чтения запроса. Проверьте формат данных.",
                "MALFORMED_JSON"
        );

        // Достаем оригинальную ошибку Jackson (причину)
        Throwable cause = ex.getCause();

        // Если причина - несовпадение типов (передали строку вместо числа)
        if (cause instanceof InvalidFormatException invalidFormatException) {
            // Jackson заботливо хранит "путь" к сломанному полю
            invalidFormatException.getPath().forEach(reference -> {
                String fieldName = reference.getFieldName();
                response.addViolation(fieldName, "Неверный тип данных. Ожидался: " + invalidFormatException.getTargetType().getSimpleName());
            });
        }
        // Если клиент прислал вообще не JSON (например, забыл закрыть скобку '}')
        else {
            response.addViolation("body", "Некорректный синтаксис JSON");
        }
        return response;
    }

    /**
     * Handler для обработки ошибки парсинга входящих параметров. Например, ожидалось число, а передали String
     * @param ex исключение
     * @return ApiErrorResponse
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ApiErrorResponse response = new ApiErrorResponse(
                "Некорректный тип параметра запроса",
                "INVALID_ARGUMENT_TYPE"
        );

        String fieldName = ex.getName() != null ? ex.getName() : "argument";
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        response.addViolation(fieldName, "Неверный тип данных. Ожидался: " + expectedType);
        return response;
    }

    @ExceptionHandler(MissedRequiredArgument.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleMissedRequiredArgument(MissedRequiredArgument ex) {
        return new ApiErrorResponse(ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleBadRequest(BadRequestException ex) {
        return new ApiErrorResponse(ex.getMessage(), ex.getCode());
    }

}
