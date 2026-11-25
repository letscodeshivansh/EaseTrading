package com.easetrading.api.user;

import com.easetrading.api.common.CurrentUserService;
import com.easetrading.api.config.AppProperties;
import com.easetrading.api.security.JwtService;
import com.easetrading.api.user.AuthDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * Login endpoints (the only ones open without being logged in):
 *   GET  /api/auth/config    -> Google client ID for the login page (null = no Google button)
 *   POST /api/auth/register  -> create account + log in
 *   POST /api/auth/login     -> email + password
 *   POST /api/auth/google    -> Google sign-in (creates the account on first use)
 *   POST /api/auth/logout    -> clear the login cookie
 *   GET  /api/auth/me        -> who is logged in (401 if nobody)
 *
 * On success the login token is put in an httpOnly cookie: the browser sends it
 * automatically and page scripts can't read it.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String COOKIE = "et_session";

    private final AuthService auth;
    private final JwtService jwt;
    private final GoogleTokenVerifier google;
    private final CurrentUserService currentUser;
    private final Duration sessionLength;

    public AuthController(AuthService auth, JwtService jwt, GoogleTokenVerifier google,
                          CurrentUserService currentUser, AppProperties props) {
        this.auth = auth;
        this.jwt = jwt;
        this.google = google;
        this.currentUser = currentUser;
        this.sessionLength = Duration.ofMinutes(props.getSecurity().getJwtExpiryMinutes());
    }

    @GetMapping("/config")
    public AuthConfig config() {
        return new AuthConfig(google.clientId());
    }

    @PostMapping("/register")
    public Me register(@Valid @RequestBody RegisterRequest req, HttpServletRequest http, HttpServletResponse res) {
        return startSession(auth.register(req.email(), req.password()), http, res);
    }

    @PostMapping("/login")
    public Me login(@Valid @RequestBody LoginRequest req, HttpServletRequest http, HttpServletResponse res) {
        return startSession(auth.login(req.email(), req.password()), http, res);
    }

    @PostMapping("/google")
    public Me google(@Valid @RequestBody GoogleRequest req, HttpServletRequest http, HttpServletResponse res) {
        return startSession(auth.loginWithGoogle(req.credential()), http, res);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest http, HttpServletResponse res) {
        res.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO, http).toString());
    }

    @GetMapping("/me")
    public Me me() {
        User u = auth.get(currentUser.currentUserId());
        return new Me(u.getEmail(), u.getRole().name());
    }

    private Me startSession(User u, HttpServletRequest http, HttpServletResponse res) {
        String token = jwt.issueToken(u.getId().toString(), u.getRole().name());
        res.addHeader(HttpHeaders.SET_COOKIE, cookie(token, sessionLength, http).toString());
        return new Me(u.getEmail(), u.getRole().name());
    }

    /** httpOnly + SameSite=Lax; marked Secure whenever the page came over HTTPS (Tailscale, the domain). */
    private static ResponseCookie cookie(String value, Duration maxAge, HttpServletRequest http) {
        boolean https = http.isSecure() || "https".equalsIgnoreCase(http.getHeader("X-Forwarded-Proto"));
        return ResponseCookie.from(COOKIE, value)
                .httpOnly(true).secure(https).sameSite("Lax").path("/").maxAge(maxAge).build();
    }
}
