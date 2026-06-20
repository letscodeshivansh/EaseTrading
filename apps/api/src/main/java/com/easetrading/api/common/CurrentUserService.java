package com.easetrading.api.common;

import com.easetrading.api.user.Role;
import com.easetrading.api.user.User;
import com.easetrading.api.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves "who is making this request".
 *
 *  - If the request carries a valid JWT, returns that user's id (the real path).
 *  - If not (single-user local dev), falls back to a seeded "dev user" so the app is
 *    usable without logging in.
 *
 * This is the seam that makes the system multi-user ready: all order/portfolio/alert
 * data is scoped by the id this returns. In production you would remove the dev
 * fallback and require authentication.
 */
@Service
public class CurrentUserService {

    private static final String DEV_EMAIL = "dev@easetrading.local";

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public CurrentUserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
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
        return devUser().getId();
    }

    /** Lazily creates the local dev user the first time it's needed. */
    private synchronized User devUser() {
        return users.findByEmail(DEV_EMAIL).orElseGet(() -> {
            User u = new User(DEV_EMAIL, encoder.encode(UUID.randomUUID().toString()));
            return users.save(u);
        });
    }
}
