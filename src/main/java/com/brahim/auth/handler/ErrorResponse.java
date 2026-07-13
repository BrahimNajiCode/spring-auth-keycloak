package com.brahim.auth.handler;

import lombok.*;

import java.util.List;
@Getter @ToString
@AllArgsConstructor @NoArgsConstructor
@Builder
public class ErrorResponse {
    private String message;
    private String code;
    private List<ValidationError> errors;
    // Nested Class
    @Getter @Setter
    @AllArgsConstructor @NoArgsConstructor
    @Builder
    public static class ValidationError{
        private String field;
        private String code;
        private String message;

    }
}
