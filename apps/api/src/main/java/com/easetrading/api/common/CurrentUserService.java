package com.easetrading.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * "Who is making this request?" — the logged-in user's id from the login cookie.
 * Every watchlist / alert / (later) order row is scoped by this id, so each user
 * only ever sees their own data. Not logged in = 401.
 */
@Service
public class CurrentUserService {

    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {
                // "anonymousUser" or anything else that isn't a user id.
            }
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in");
    }
}
