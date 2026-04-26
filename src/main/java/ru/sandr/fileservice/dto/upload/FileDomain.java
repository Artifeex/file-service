package ru.sandr.fileservice.dto.upload;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum FileDomain {

    USER_AVATAR(
            Set.of("image/jpeg", "image/png"),
            1024 * 1024 * 2 // 2мб ограничение на аватар пользователя
    ),
    COURSE_AVATAR(
            Set.of("image/jpeg", "image/png"),
            1024 * 1024 * 2 // 2мб ограничение на аватар пользователя
    ),
    COURSE_MATERIAL(
            Set.of("image/jpeg", "image/png"), // Добавить еще типы docs, pdf, mp4
            1024 * 1024 * 200 // 200мб ограничение на размер материала курса
    ),
    ANSWER_FILE(
            Set.of("image/jpeg", "image/png"), // Не забыть добавить другие типы файлов
            1024 * 1024 * 5 // 5мб ограничение на ответ
    );

    private final Set<String> allowedContentTypes;
    private final long maxSizeBytes;

    public boolean validateContentType(String contentType) {
        return allowedContentTypes.contains(contentType.toLowerCase());
    }

}
