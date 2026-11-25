package com.easetrading.api.common;

import com.easetrading.api.user.User;
import com.easetrading.api.user.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves "who is making this request".
 *
 *  - If the request carries a valid JWT, returns that user's id (the real path).
 *  - If not (no login screen yet), falls back to a "dev user" so the app works
 *    without logging in. Remove this fallback once the login screen exists.
 *
 * The dev user is created once at startup and its id kept in memory, so requests
 * never touch the database just to find out who they are (a lazy, locked lookup
 * here deadlocked with SQLite — bug B26).
 */
@Service
@Order(0)
public class CurrentUserService implements ApplicationRunner {

    private static final String DEV_EMAIL = "dev@easetrading.local";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private volatile UUID devUserId;

    public CurrentUserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        devUserId = users.findByEmail(DEV_EMAIL)
                .orElseGet(() -> users.save(new User(DEV_EMAIL, encoder.encode(UUID.randomUUID().toString()))))
                .getId();
    }

    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {
                // Fall through to the dev user.
            }
        }
        if (devUserId == null) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Starting up — try again in a moment");
        return devUserId;
    }
}
