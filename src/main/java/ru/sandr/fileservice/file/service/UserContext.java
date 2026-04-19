package ru.sandr.fileservice.file.service;

import java.util.Set;
import java.util.UUID;

public record UserContext(
        UUID userId,
        Set<String> roles
) {
}
