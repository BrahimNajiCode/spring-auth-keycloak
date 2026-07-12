package com.brahim.auth.dto.request;

import jakarta.validation.constraints.NotBlank;


/**
 * Incoming payload for POST /api/v1/auth/login.
 *
 * This request is forwarded to Keycloak's token endpoint using the
 * Resource Owner Password Credentials (ROPC) flow.
 *
 * NOTE ON ROPC:
 *   ROPC is convenient for first-party apps (your own mobile/web app)
 *   but is deprecated in OAuth 2.1. Prefer Authorization Code + PKCE for
 *   true OAuth. For a learning project, ROPC is perfectly fine.
 */
public record LoginRequest(
    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    String password
) {}
