package com.easetrading.api.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response shapes for login. The login token itself travels in a cookie, never in JSON. */
public class AuthDtos {

    public record RegisterRequest(
            @Email(message = "Enter a valid email") @NotBlank String email,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password
    ) {}

    public record LoginRequest(
            @Email(message = "Enter a valid email") @NotBlank String email,
            @NotBlank String password
    ) {}

    public record GoogleRequest(@NotBlank String credential) {}

    /** Who is logged in. */
    public record Me(String email, String role) {}

    /** What the login page needs: the Google client ID (null = hide the Google button). */
    public record AuthConfig(String googleClientId) {}
}
