package com.brahim.auth.service.impl;

import com.brahim.auth.config.KeycloakProperties;
import com.brahim.auth.dto.request.LoginRequest;
import com.brahim.auth.dto.request.RefreshTokenRequest;
import com.brahim.auth.dto.request.RegisterRequest;
import com.brahim.auth.dto.response.TokenResponse;
import com.brahim.auth.dto.response.UserResponse;
import com.brahim.auth.exception.*;
import com.brahim.auth.model.User;
import com.brahim.auth.model.UserMapper;
import com.brahim.auth.repository.UserRepository;
import com.brahim.auth.service.AuthService;
import com.brahim.auth.service.KeycloakTokenService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.brahim.auth.exception.ErrorCode.*;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 *  AuthServiceImpl: The Orchestrator
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *  This is where business logic lives. It coordinates:
 *    • Keycloak Admin Client  → user management (create, password, email)
 *    • KeycloakTokenService   → token exchange (login, refresh, logout)
 *    • UserRepository      → local database record
 *
 *  REGISTER FLOW:
 *    1. Check uniqueness (email, username) in local DB (fast, no Keycloak call)
 *    2. Create user in Keycloak via Admin REST API → get keycloakId
 *    3. Set password in Keycloak
 *    4. Send email verification (optional but production-ready)
 *    5. Save local User record linked by keycloakId
 *    6. Return UserResponse
 *
 *  @Transactional only wraps DB operations.
 *  Keycloak calls are NOT in the transaction — Keycloak is not your DB.
 *  If Keycloak succeeds but DB save fails, we attempt Keycloak user deletion
 *  (compensating transaction / saga pattern).
 */


@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository repository;
    private final KeycloakProperties props;
    private final KeycloakTokenService tokenService;
    private final Keycloak keycloak;
    private final UserMapper mapper;


    // Register
    @Override
    public UserResponse register(RegisterRequest request) {
        log.info("Registering user: {}", request.username());

        // Step 1: Check local uniqueness before calling Keycloak (fail fast)
        if(repository.existsByEmailIgnoreCase(request.email())){
            throw new BusinessException(EMAIL_ALREADY_EXISTS ,request.email());
        }
        if(repository.existsByUsername(request.username())){
            throw new BusinessException(USERNAME_ALREADY_EXISTS,request.username());
        }

        // Step 2: Create user in Keycloak
        String keycloakId = createKeycloakUser(request);
        User savedUser;
        // Step 3: Save to local DB (compensating on failure)
        try{
            User user = mapper.toUser(request, keycloakId);
            savedUser = repository.save(user);

        }catch (Exception e){
            // Saga compensation: if local save fails, remove user from Keycloak
            log.error("Local DB save failed after Keycloak user creation. Rolling back Keycloak user {}", keycloakId);
            deleteKeycloakUserSilently(keycloakId);
            throw e;
        }
        log.info("User registered successfully: {} (keycloakId={})", request.username(), keycloakId);
        return mapper.toUserResponse(savedUser);
    }

    // Login
    @Override
    public TokenResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.username());
        // Delegate entirely to Keycloak
        return tokenService.obtainToken(request.username(), request.password());
    }


    // Refresh
    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        return tokenService.refreshToken(request.refreshToken());
    }


    // Forgot-Password
    @Override
    public void forgotPassword(String email) {
        log.info("Password reset requested for: {}", email);
        repository.findByEmailIgnoreCase(email).ifPresent(user -> {
            try {
                // Trigger Keycloak's built-in forgot-password email
                getUsersResource()
                        .get(user.getKeycloakId())
                        .executeActionsEmail(List.of("UPDATE_PASSWORD"));
                log.info("Password reset email sent to: {}", email);
            } catch (Exception e) {
                log.warn("Failed to send password reset email for {}: {}", email, e.getMessage());
            }
        });
    }


    // Logout
    @Override
    public void logout(String refreshToken) {
        tokenService.revokeToken(refreshToken);
        log.info("User logged out (refresh token revoked)");
    }

    // Helpers
    private String createKeycloakUser(RegisterRequest request) {
        log.info("Building UserRepresentation for: {} ", request.username());
        UserRepresentation user = buildRepresentation(request);

        user.setCredentials(List.of(buildCredential(request.password())));

        String keycloakId;
        try (Response response = getUsersResource().create(user)) {
            if (response.getStatus() == 409) {
                throw new BusinessException(USER_ALREADY_EXISTS, request.username());
            }
            if (response.getStatus() != 201) {
                throw new BusinessException(CREATION_FAILED, request.username());
            }

            // The new user's ID is in the Location header: .../users/{id}
            String locationPath = response.getLocation().getPath();
            keycloakId = locationPath.substring(locationPath.lastIndexOf('/') + 1);
        }

        log.debug("Keycloak user created with id: {}", keycloakId);

        // Send email verification
        try {
            getUsersResource().get(keycloakId)
                    .sendVerifyEmail();
        } catch (Exception e) {
            log.warn("Could not send verification email: {}", e.getMessage());
        }

        return keycloakId;
    }

    private UserRepresentation buildRepresentation(RegisterRequest request){
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(true);
        user.setEmailVerified(false);
        return user;
    }
    private CredentialRepresentation buildCredential(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);   // true = force change on first login
        return credential;
    }

    private void deleteKeycloakUserSilently(String keycloakId) {
        try {
            getUsersResource().get(keycloakId).remove();
        } catch (Exception e) {
            log.error("Failed to delete Keycloak user during rollback: {}", e.getMessage());
        }
    }

    private UsersResource getUsersResource() {
        RealmResource realm = keycloak.realm(props.realm());
        return realm.users();
    }

}
