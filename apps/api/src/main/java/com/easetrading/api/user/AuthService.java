package com.easetrading.api.user;

import com.easetrading.api.common.ApiException;
import com.easetrading.api.security.JwtService;
import com.easetrading.api.user.AuthDtos.AuthResponse;
import com.easetrading.api.user.AuthDtos.LoginRequest;
import com.easetrading.api.user.AuthDtos.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Business logic for sign-up and login. Controllers stay thin; all the rules
 * (hashing, duplicate checks, token issuing) live here so they are easy to test.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Creates a new account and returns a session token. */
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = new User(req.email(), encoder.encode(req.password()));
        users.save(user);
        return toResponse(user);
    }

    /** Verifies credentials and returns a session token. */
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        String token = jwt.issueToken(user.getId().toString(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}
