package com.brahim.auth.service;

import com.brahim.auth.dto.request.LoginRequest;
import com.brahim.auth.dto.request.RefreshTokenRequest;
import com.brahim.auth.dto.request.RegisterRequest;
import com.brahim.auth.dto.response.TokenResponse;
import com.brahim.auth.dto.response.UserResponse;

/**
 * Contract for all authentication operations.
 *
 * SENIOR TIP — WHY program to an interface?
 *   1. Controllers depend on AuthService (the abstraction), not AuthServiceImpl.
 *      Swap the implementation without touching controllers or tests.
 *   2. @MockBean in tests mocks the interface, not the class.
 *   3. AOP proxies (for @Transactional, @Cacheable) only work reliably on interfaces
 *      unless CGLIB subclassing is enabled.
 *   4. Forces you to think about the API contract before the implementation.
 */

public interface AuthService {
    /** Register a new user in Keycloak AND in the local database. */
    UserResponse register(RegisterRequest request);

    /** Authenticate via Keycloak ROPC flow, return tokens. */
    TokenResponse login(LoginRequest request);

    /** Exchange a refresh token for new access + refresh tokens. */
    TokenResponse refresh(RefreshTokenRequest request);

    /** Trigger Keycloak's built-in forgot-password email flow. */
    void forgotPassword(String email);

    /** Log out a user by invalidating their session in Keycloak. */
    void logout(String refreshToken);

}
