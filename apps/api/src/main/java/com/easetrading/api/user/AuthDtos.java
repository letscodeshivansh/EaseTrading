package com.easetrading.api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request/response shapes for authentication. Java "records" are immutable data
 * carriers — perfect for DTOs. Validation annotations reject bad input early.
 */
public class AuthDtos {

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    /** Returned on successful login/register. The token goes in the Authorization header. */
    public record AuthResponse(String token, String email, String role) {}
}
