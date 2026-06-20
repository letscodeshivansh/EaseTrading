package com.easetrading.api.user;

import com.easetrading.api.user.AuthDtos.AuthResponse;
import com.easetrading.api.user.AuthDtos.LoginRequest;
import com.easetrading.api.user.AuthDtos.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Public authentication endpoints.
 *   POST /api/auth/register  -> create an account
 *   POST /api/auth/login     -> obtain a session token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
