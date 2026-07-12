package com.brahim.auth.controller;

import com.brahim.auth.dto.request.LoginRequest;
import com.brahim.auth.dto.request.RefreshTokenRequest;
import com.brahim.auth.dto.request.RegisterRequest;
import com.brahim.auth.dto.response.TokenResponse;
import com.brahim.auth.dto.response.UserResponse;
import com.brahim.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * REST controller for authentication endpoints.
 *
 * SENIOR TIPS applied here:
 *   1. Controllers are THIN — no business logic. They receive, validate, delegate, respond.
 *   2. @Valid triggers Bean Validation on the request body. If validation fails,
 *      GlobalExceptionHandler intercepts the MethodArgumentNotValidException.
 *   3. ResponseEntity<T> gives explicit control over HTTP status codes.
 *   4. Versioned URL (/api/v1/) — changing the API never breaks existing clients.
 *   5. @RequestMapping at class level → DRY, all methods share the base path.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @RequestBody @Valid RegisterRequest request
    ){
        UserResponse response = service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @RequestBody @Valid LoginRequest request
    ){
        TokenResponse tokens = service.login(request);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestBody @Valid RefreshTokenRequest request
    ){
        TokenResponse tokens = service.refresh(request);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @RequestParam @Email(message = "Must be a valid email") String email
    ){
        service.forgotPassword(email);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody @Valid RefreshTokenRequest request
    ){
        service.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
