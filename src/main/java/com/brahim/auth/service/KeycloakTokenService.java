package com.brahim.auth.service;

import com.brahim.auth.dto.response.TokenResponse;

/**
 * Abstracts all direct HTTP calls to Keycloak's token endpoint.
 *
 * WHY a separate service?
 *   The token endpoint is HTTP + form-encoded — network-level concerns
 *   that don't belong in AuthServiceImpl alongside business logic.
 *   Separating them respects the Single Responsibility Principle.
 */
public interface KeycloakTokenService {

    /** Resource Owner Password Credentials grant. */
    TokenResponse obtainToken(String username, String password);

    /** Refresh grant. */
    TokenResponse refreshToken(String refreshToken);

    /** Revokes the refresh token (logout). */
    void revokeToken(String refreshToken);
}
