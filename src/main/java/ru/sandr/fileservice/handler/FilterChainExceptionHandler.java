package ru.sandr.fileservice.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ru.sandr.fileservice.exception.UnauthorizedException;

@Component
public class FilterChainExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    // Внедряем тот самый "мост" из Spring MVC
    private final HandlerExceptionResolver resolver;

    // Обязательно используем @Qualifier, так как реализаций HandlerExceptionResolver
    // может быть несколько, нам нужен главный MVC-шный
    public FilterChainExceptionHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    // Обработка ошибки 401 (Не авторизован)
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) {
        // Делегируем обработку в ControllerAdvice
        resolver.resolveException(
                request,
                response,
                null,
                new UnauthorizedException("NO_AUTHORIZED", "Пользователь не авторизован")
        );
    }

    // Обработка ошибки 403 (Нет прав)
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) {
        // Делегируем обработку в ControllerAdvice
        resolver.resolveException(
                request,
                response,
                null,
                new ru.sandr.fileservice.exception.AccessDeniedException(
                        "FORBIDDEN",
                        "У пользователя недостаточно прав для выполнения операции"
                )
        );
    }
}
