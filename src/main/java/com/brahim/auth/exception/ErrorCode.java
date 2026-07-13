package com.brahim.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
public enum ErrorCode {
    USER_ALREADY_EXISTS("KEYCLOAK", "User %s already exists in keycloak", CONFLICT),
    CREATION_FAILED("KEYCLOAK", "Keycloak user %s creation failed: HTTP", CONFLICT),
    EMPTY_RESPONSE("KEYCLOAK", "Empty response from Keycloak", BAD_GATEWAY),
    AUTH_FAILED("KEYCLOAK", "Authentication failed: %s ", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS("EMAIL", "Email already registered: %s ", CONFLICT),
    EMAIL_NOT_FOUND("EMAIL", "No account found for email: %s", NOT_FOUND),
    USERNAME_ALREADY_EXISTS("USER", "Username already taken: %s ", CONFLICT),
    USER_NOT_FOUND("USER", "User not found: %s ", NOT_FOUND),
    UNAUTHORIZED("SECURITY", "No authenticated JWT found in SecurityContext", HttpStatus.UNAUTHORIZED),
    INTERNAL_EXCEPTION("INTERNAL_EXCEPTION",
            "An internal exception occurred, please try again or contact the admin",
            HttpStatus.INTERNAL_SERVER_ERROR),


    ;
    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }
}
