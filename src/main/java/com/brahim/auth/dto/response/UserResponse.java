package com.brahim.auth.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Public representation of a user — never expose the internal User entity directly.
 *
 * SENIOR TIP:
 *   Returning your @Entity directly from controller couples your API contract
 *   to your database schema. Any schema change breaks your API.
 *   Always map to a DTO (or record) in the service layer.
 */
public record UserResponse(
        String id,
        String keycloakId,
        String username,
        String email,
        String firstName,
        String lastName,
        List<String> roles,
        boolean emailVerified,
        Instant createdAt
) {}
