package com.brahim.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Incoming payload for POST /api/v1/auth/register.
 *
 * Records are immutable by design — ideal for DTOs.
 * @NotBlank, @Email etc. are checked by @Valid in the controller
 * before the service layer is ever called.
 */
public record RegisterRequest(
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    String firstName,


    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    String lastName,


    @NotBlank(message = "Email name is required")
    @Email(message = "Email must be valid")
    String email,


    @NotBlank(message = "Username name is required")
    @Size(min = 3, max = 30, message = "Username must be 3–30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username may only contain letters, digits, _ and -")
    String username,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password

) {}
