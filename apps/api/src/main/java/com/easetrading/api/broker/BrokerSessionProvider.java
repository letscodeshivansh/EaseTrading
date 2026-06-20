package com.easetrading.api.broker;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Caches the Angel One session so we log in once and reuse the tokens, instead of
 * authenticating on every request. Angel One tokens are valid for several hours;
 * we refresh proactively after a fixed window.
 *
 * For Prompt 1 this manages the single default (dev) session. In a multi-user build
 * there would be one provider entry per user.
 *
 * Thread-safety: login() is synchronized because the market-data ingester (a
 * background thread) and HTTP requests may both ask for the session at once.
 */
@Component
public class BrokerSessionProvider {

    private static final Duration REFRESH_AFTER = Duration.ofHours(6);

    private final AngelOneAdapter angelOne;

    private volatile AngelOneSession session;
    private volatile Instant loggedInAt;

    public BrokerSessionProvider(AngelOneAdapter angelOne) {
        this.angelOne = angelOne;
    }

    /** Returns a valid session, logging in (or refreshing) only when needed. */
    public synchronized AngelOneSession get() {
        boolean expired = loggedInAt == null
                || Duration.between(loggedInAt, Instant.now()).compareTo(REFRESH_AFTER) > 0;

        if (session == null || !session.isValid() || expired) {
            session = angelOne.login();
            loggedInAt = Instant.now();
        }
        return session;
    }

    /** Forces a fresh login on the next get() — used after a 401 from Angel One. */
    public synchronized void invalidate() {
        session = null;
        loggedInAt = null;
    }
}
