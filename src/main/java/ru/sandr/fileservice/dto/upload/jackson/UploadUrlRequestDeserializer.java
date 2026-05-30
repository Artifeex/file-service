package ru.sandr.fileservice.dto.upload.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.sandr.fileservice.dto.upload.FileDomain;
import ru.sandr.fileservice.dto.upload.UploadUrlRequest;
import ru.sandr.fileservice.dto.upload.context.FileContext;

import java.io.IOException;

public class UploadUrlRequestDeserializer extends JsonDeserializer<UploadUrlRequest> {

    @Override
    public UploadUrlRequest deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();
        JsonNode node = mapper.readTree(parser);

        String originalFilename = requiredText(node, "originalFilename");
        FileDomain domain = mapper.convertValue(required(node, "domain"), FileDomain.class);
        String contentType = requiredText(node, "contentType");
        long contentLength = required(node, "contentLength").asLong();
        FileContext fileContext = FileContextMapper.map(domain, required(node, "context"), mapper);

        return new UploadUrlRequest(originalFilename, domain, contentType, contentLength, fileContext);
    }

    private static JsonNode required(JsonNode node, String fieldName) throws JsonMappingException {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            throw JsonMappingException.from(
                    node.traverse(),
                    fieldName + " is required"
            );
        }
        return value;
    }

    private static String requiredText(JsonNode node, String fieldName) throws JsonMappingException {
        return required(node, fieldName).asText();
    }
}
