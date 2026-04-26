package ru.sandr.fileservice.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import ru.sandr.fileservice.exception.UnauthorizedException;

@Component
public class UserContextExtractor {

    public UserContext extract(Jwt jwt) {
        if (jwt == null) {
            throw new UnauthorizedException("JWT token is missing");
        }

        UUID userId = extractUserId(jwt);
        Set<String> roles = extractRoles(jwt);
        if (roles.isEmpty()) {
            throw new UnauthorizedException("JWT does not contain roles");
        }
        return new UserContext(userId, roles);
    }

    private UUID extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaims().get("user_id");
        if (userIdClaim != null) {
            return parseUuid(userIdClaim.toString(), "user_id");
        }
        if (jwt.getSubject() != null) {
            return parseUuid(jwt.getSubject(), "sub");
        }
        throw new UnauthorizedException("JWT does not contain user identifier");
    }

    private UUID parseUuid(String value, String claimName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException("Invalid UUID in JWT claim: " + claimName);
        }
    }

    private Set<String> extractRoles(Jwt jwt) {
        Set<String> rolesFromRolesClaim = extractRolesFromClaim(jwt.getClaim("roles"));
        if (!rolesFromRolesClaim.isEmpty()) {
            return rolesFromRolesClaim;
        }

        String scope = jwt.getClaimAsString("scope");
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(scope.split(" "))
                .filter(token -> token.startsWith("ROLE_"))
                .map(this::normalizeRole)
                .collect(Collectors.toSet());
    }

    private Set<String> extractRolesFromClaim(Object claim) {
        if (!(claim instanceof Collection<?> collection)) {
            return Set.of();
        }

        return collection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(this::normalizeRole)
                .collect(Collectors.toSet());
    }

    private String normalizeRole(String role) {
        return role.replace("ROLE_", "").toUpperCase(Locale.ROOT);
    }
}
