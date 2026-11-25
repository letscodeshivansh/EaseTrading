package com.easetrading.api.account;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A user's linked Groww account: their API key and secret, stored ENCRYPTED
 * (AES-GCM, see CredentialCipher). One row per user.
 *
 * keyHint keeps the last 4 characters of the API key so the settings page can show
 * which key is saved without ever sending the key back.
 */
@Entity
@Table(name = "broker_account", uniqueConstraints = @UniqueConstraint(columnNames = {"userId"}))
public class BrokerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String broker = "GROWW";

    /** approval (API key + secret) | totp (API key + TOTP secret) */
    @Column(nullable = false)
    private String authMode;

    @Column(nullable = false)
    private byte[] apiKeyEnc;

    @Column(nullable = false)
    private byte[] secretEnc;

    private String keyHint;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected BrokerAccount() { }

    public BrokerAccount(UUID userId) {
        this.userId = userId;
    }

    public void update(String authMode, byte[] apiKeyEnc, byte[] secretEnc, String keyHint) {
        this.authMode = authMode;
        this.apiKeyEnc = apiKeyEnc;
        this.secretEnc = secretEnc;
        this.keyHint = keyHint;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public String getBroker() { return broker; }
    public String getAuthMode() { return authMode; }
    public byte[] getApiKeyEnc() { return apiKeyEnc; }
    public byte[] getSecretEnc() { return secretEnc; }
    public String getKeyHint() { return keyHint; }
    public Instant getUpdatedAt() { return updatedAt; }
}
