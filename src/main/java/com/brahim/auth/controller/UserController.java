package com.brahim.auth.controller;


import com.brahim.auth.dto.response.UserResponse;
import com.brahim.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Protected resource endpoints.
 *
 * @PreAuthorize is evaluated BEFORE the method runs (hence "pre").
 * It uses Spring Expression Language (SpEL) against the SecurityContext.
 *
 * hasRole('USER')  → checks for GrantedAuthority "ROLE_USER"
 * hasRole('ADMIN') → checks for GrantedAuthority "ROLE_ADMIN"
 * isAuthenticated() → any valid JWT, any role
 *
 * These work because JwtAuthenticationConverter populated the authorities.
 */

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;


    /** GET /api/v1/users/me — any authenticated user */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getCurrentUser(){
        return ResponseEntity.ok(service.getCurrentUser());
    }

    /** GET /api/v1/users/{keycloakId} — admin only */
    @GetMapping("/{keycloak-id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable("keycloak-id") String keycloakId
    ){
        return ResponseEntity.ok(service.getUserByKeycloakId(keycloakId));
    }


    /** POST /api/v1/users/{keycloakId}/roles/{roleName} — admin only */
    @PostMapping("/{keycloakId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignRole(
            @PathVariable String keycloakId,
            @PathVariable String roleName
    ){
        service.assignRole(keycloakId, roleName);
        return ResponseEntity.noContent().build();
    }


    /** DELETE /api/v1/users/{keycloakId}/roles/{roleName} — admin only */
    @DeleteMapping("/{keycloakId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeRole(
            @PathVariable String keycloakId,
            @PathVariable String roleName
    ){
        service.removeRole(keycloakId, roleName);
        return ResponseEntity.noContent().build();
    }
}
