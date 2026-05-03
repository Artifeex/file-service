package ru.sandr.fileservice.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CommitFileRequest(
        @NotNull UUID fileId
) {
}
