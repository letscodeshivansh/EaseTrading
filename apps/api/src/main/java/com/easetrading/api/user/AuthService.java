package com.easetrading.api.user;

import com.easetrading.api.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sign up, log in (email + password) and Google sign-in.
 *
 * After 5 wrong passwords for an email, that email is locked for 15 minutes —
 * enough to stop password guessing without bothering real users.
 */
@Service
public class AuthService {

    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK = Duration.ofMinutes(15);

    private record Failures(int count, Instant lockedUntil) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final GoogleTokenVerifier google;
    private final Map<String, Failures> failures = new ConcurrentHashMap<>();

    public AuthService(UserRepository users, PasswordEncoder encoder, GoogleTokenVerifier google) {
        this.users = users;
        this.encoder = encoder;
        this.google = google;
    }

    public User register(String email, String password) {
        String e = email.trim().toLowerCase();
        if (users.existsByEmail(e)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists — log in instead");
        }
        return users.save(new User(e, encoder.encode(password)));
    }

    public User login(String email, String password) {
        String e = email.trim().toLowerCase();
        Failures f = failures.get(e);
        if (f != null && f.lockedUntil() != null && Instant.now().isBefore(f.lockedUntil())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong passwords — try again in 15 minutes");
        }
        User user = users.findByEmail(e).orElse(null);
        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            int count = f == null ? 1 : f.count() + 1;
            failures.put(e, new Failures(count, count >= MAX_FAILURES ? Instant.now().plus(LOCK) : null));
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        failures.remove(e);
        return user;
    }

    /** Google sign-in: log in, or create the account on first use (no password needed). */
    public User loginWithGoogle(String credential) {
        String email = google.verifiedEmail(credential);
        return users.findByEmail(email).orElseGet(() ->
                // Random password: a Google-only account can't be logged into with a password.
                users.save(new User(email, encoder.encode(UUID.randomUUID().toString()))));
    }

    public User get(UUID id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in"));
    }
}
