package com.easetrading.api.broker;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A user's Angel One credentials, stored ENCRYPTED (the *_enc columns are AES-GCM
 * ciphertext produced by CryptoService). We never persist plain-text secrets.
 *
 * One row per user keeps the design multi-user ready from day one.
 */
@Entity
@Table(name = "broker_credentials")
public class BrokerCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String broker = "ANGEL_ONE";

    @Lob @Column(nullable = false) private byte[] apiKeyEnc;
    @Lob @Column(nullable = false) private byte[] clientIdEnc;
    @Lob @Column(nullable = false) private byte[] passwordEnc;
    @Lob @Column(nullable = false) private byte[] totpSecretEnc;

    /** DISCONNECTED | CONNECTED — last known broker session state. */
    @Column(nullable = false)
    private String status = "DISCONNECTED";

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected BrokerCredential() { }

    public BrokerCredential(UUID userId, byte[] apiKeyEnc, byte[] clientIdEnc,
                            byte[] passwordEnc, byte[] totpSecretEnc) {
        this.userId = userId;
        this.apiKeyEnc = apiKeyEnc;
        this.clientIdEnc = clientIdEnc;
        this.passwordEnc = passwordEnc;
        this.totpSecretEnc = totpSecretEnc;
    }

    public UUID getUserId() { return userId; }
    public byte[] getApiKeyEnc() { return apiKeyEnc; }
    public byte[] getClientIdEnc() { return clientIdEnc; }
    public byte[] getPasswordEnc() { return passwordEnc; }
    public byte[] getTotpSecretEnc() { return totpSecretEnc; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.updatedAt = Instant.now(); }
}
