package ru.sandr.fileservice.service;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public record UserContext(
        UUID userId,
        Set<String> roles
) {
}
