package ru.sandr.fileservice.dto.upload.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.sandr.fileservice.dto.upload.FileDomain;
import ru.sandr.fileservice.dto.upload.context.CourseAvatarContext;
import ru.sandr.fileservice.dto.upload.context.CourseMaterialContext;
import ru.sandr.fileservice.dto.upload.context.FileContext;
import ru.sandr.fileservice.dto.upload.context.TaskAnswerContext;
import ru.sandr.fileservice.dto.upload.context.UserAvatarContext;

public final class FileContextMapper {

    private FileContextMapper() {
    }

    public static FileContext map(FileDomain domain, JsonNode contextNode, ObjectMapper mapper) {
        return switch (domain) {
            case USER_AVATAR -> mapper.convertValue(contextNode, UserAvatarContext.class);
            case COURSE_AVATAR -> mapper.convertValue(contextNode, CourseAvatarContext.class);
            case COURSE_MATERIAL -> mapper.convertValue(contextNode, CourseMaterialContext.class);
            case ANSWER_FILE -> mapper.convertValue(contextNode, TaskAnswerContext.class);
        };
    }
}
