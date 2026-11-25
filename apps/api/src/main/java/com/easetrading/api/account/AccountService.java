package com.easetrading.api.account;

import com.easetrading.api.audit.AuditService;
import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.broker.groww.GrowwAccountClient;
import com.easetrading.api.broker.groww.GrowwCredentials;
import com.easetrading.api.broker.groww.GrowwSession;
import com.easetrading.api.common.ApiException;
import com.easetrading.api.config.AppProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Each user's own broker account: link Groww (Settings), then their holdings,
 * funds and positions come from THEIR Groww login.
 *
 *   demo mode  (BROKER_PROVIDER=mock)  : every user sees the same demo account.
 *   groww mode (BROKER_PROVIDER=groww) : each user links their own API key + secret.
 *
 * Keys are stored encrypted (CredentialCipher) and are never sent back to the
 * browser. One Groww session per user is kept in memory; like everywhere else,
 * a failed login is tried again only when the user presses Reconnect.
 */
@Service
public class AccountService {

    private static final Set<String> MODES = Set.of("approval", "totp");

    private final AppProperties props;
    private final BrokerAccountRepository accounts;
    private final CredentialCipher cipher;
    private final AuditService audit;
    private final BrokerAdapter marketData;
    private final ObjectProvider<GrowwAccountClient> groww;
    private final Map<UUID, GrowwSession> sessions = new ConcurrentHashMap<>();

    public AccountService(AppProperties props, BrokerAccountRepository accounts, CredentialCipher cipher,
                          AuditService audit, BrokerAdapter marketData, ObjectProvider<GrowwAccountClient> groww) {
        this.props = props;
        this.accounts = accounts;
        this.cipher = cipher;
        this.audit = audit;
        this.marketData = marketData;
        this.groww = groww;
    }

    /** What the settings page shows. Never contains the key or secret. */
    public record AccountView(String provider, boolean linked, String authMode, String keyHint, Instant updatedAt) {}

    /** Settings form. `secret` = API secret (approval) or TOTP secret (totp). */
    public record LinkRequest(String authMode, String apiKey, String secret) {}

    private boolean demo() {
        return !props.getBroker().isGroww();
    }

    // ------------------------------------------------------------------ linking

    public AccountView view(UUID userId) {
        if (demo()) return new AccountView("MOCK", true, null, null, null);
        return accounts.findByUserId(userId)
                .map(a -> new AccountView("GROWW", true, a.getAuthMode(), a.getKeyHint(), a.getUpdatedAt()))
                .orElse(new AccountView("GROWW", false, null, null, null));
    }

    /** Save (or replace) the user's Groww keys, then try to log in once. */
    @Transactional
    public BrokerStatus link(UUID userId, LinkRequest req) {
        if (demo()) throw new ApiException(HttpStatus.BAD_REQUEST, "Demo mode — there is no real broker to link");
        String mode = req.authMode() == null ? "approval" : req.authMode().trim().toLowerCase();
        if (!MODES.contains(mode)) throw new ApiException(HttpStatus.BAD_REQUEST, "Login type must be approval or totp");
        String key = trim(req.apiKey());
        String secret = trim(req.secret());
        if (key.isEmpty() || secret.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter both the API key and the " + (mode.equals("totp") ? "TOTP secret" : "API secret"));
        }

        BrokerAccount account = accounts.findByUserId(userId).orElseGet(() -> new BrokerAccount(userId));
        String hint = key.length() <= 4 ? "****" : "…" + key.substring(key.length() - 4);
        account.update(mode, cipher.encrypt(key), cipher.encrypt(secret), hint);
        accounts.save(account);
        sessions.put(userId, groww.getObject().newSession(new GrowwCredentials(mode, key, secret)));
        audit.log(userId, "BROKER_LINKED", "BROKER_ACCOUNT", userId.toString(), "GROWW " + mode + " key " + hint);
        return status(userId);
    }

    @Transactional
    public void unlink(UUID userId) {
        accounts.findByUserId(userId).ifPresent(accounts::delete);
        sessions.remove(userId);
        audit.log(userId, "BROKER_UNLINKED", "BROKER_ACCOUNT", userId.toString(), "");
    }

    /** Reconnect button: log in again now (after approving the key on Groww). */
    public BrokerStatus reconnect(UUID userId) {
        marketData.reconnect();
        if (!demo() && accounts.findByUserId(userId).isPresent()) {
            try {
                session(userId).reconnect();
            } catch (Exception ignored) {
                // status() below explains what went wrong
            }
        }
        return status(userId);
    }

    // ------------------------------------------------------------------ account data

    /** Is this user's broker reachable right now? Never throws. */
    public BrokerStatus status(UUID userId) {
        if (demo()) return new BrokerStatus("MOCK", true, true, null, "Demo data — the server runs without a real broker");
        if (accounts.findByUserId(userId).isEmpty()) {
            return new BrokerStatus("GROWW", false, false, null, "Link your Groww account in Settings");
        }
        try {
            GrowwSession s = session(userId);
            BrokerProfile p = groww.getObject().profile(s);
            String id = p.clientId() == null ? "" : " as ****" + p.clientId().substring(Math.max(0, p.clientId().length() - 4));
            return new BrokerStatus("GROWW", true, true, s.expiresAt(), "Connected" + id);
        } catch (Exception e) {
            return new BrokerStatus("GROWW", true, false, null, e.getMessage());
        }
    }

    public BrokerFunds funds(UUID userId) {
        return demo() ? new BrokerFunds(100_000, 0, 100_000, 0) : groww.getObject().funds(session(userId));
    }

    public List<BrokerHolding> holdings(UUID userId) {
        return demo() ? DEMO_HOLDINGS : groww.getObject().holdings(session(userId));
    }

    public List<BrokerPosition> positions(UUID userId) {
        return demo() ? List.of() : groww.getObject().positions(session(userId));
    }

    /** The user's Groww session, built from their saved (decrypted) keys on first use. */
    private GrowwSession session(UUID userId) {
        return sessions.computeIfAbsent(userId, id -> {
            BrokerAccount a = accounts.findByUserId(id).orElseThrow(() ->
                    new ApiException(HttpStatus.CONFLICT, "Link your Groww account in Settings first"));
            return groww.getObject().newSession(new GrowwCredentials(
                    a.getAuthMode(), cipher.decrypt(a.getApiKeyEnc()), cipher.decrypt(a.getSecretEnc())));
        });
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static final List<BrokerHolding> DEMO_HOLDINGS = List.of(
            new BrokerHolding("RELIANCE", "INE002A01018", 10, 1350.0, 0),
            new BrokerHolding("TCS", "INE467B01029", 4, 3620.0, 0),
            new BrokerHolding("HDFCBANK", "INE040A01034", 12, 1580.0, 0),
            new BrokerHolding("INFY", "INE009A01021", 8, 1610.0, 0));
}
