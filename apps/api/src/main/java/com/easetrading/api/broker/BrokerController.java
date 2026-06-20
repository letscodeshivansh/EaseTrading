package com.easetrading.api.broker;

import com.easetrading.api.security.CryptoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints to connect a user's Angel One account and check its status.
 * Credentials are encrypted before they ever touch the database.
 *
 *   POST /api/broker/connect  -> store encrypted credentials
 *   GET  /api/broker/status   -> CONNECTED / DISCONNECTED
 */
@RestController
@RequestMapping("/api/broker")
public class BrokerController {

    private final BrokerCredentialRepository repo;
    private final CryptoService crypto;

    public BrokerController(BrokerCredentialRepository repo, CryptoService crypto) {
        this.repo = repo;
        this.crypto = crypto;
    }

    public record ConnectRequest(
            @NotBlank String apiKey,
            @NotBlank String clientId,
            @NotBlank String password,
            @NotBlank String totpSecret) {}

    @PostMapping("/connect")
    public Map<String, String> connect(Authentication auth, @Valid @RequestBody ConnectRequest req) {
        UUID userId = UUID.fromString(auth.getName()); // the JWT subject is the user id

        // Encrypt every secret field individually before saving.
        BrokerCredential cred = repo.findByUserId(userId).orElse(
                new BrokerCredential(userId,
                        crypto.encrypt(req.apiKey()),
                        crypto.encrypt(req.clientId()),
                        crypto.encrypt(req.password()),
                        crypto.encrypt(req.totpSecret())));
        cred.setStatus("DISCONNECTED"); // becomes CONNECTED after first successful login
        repo.save(cred);

        return Map.of("status", "saved");
    }

    @GetMapping("/status")
    public Map<String, String> status(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        String status = repo.findByUserId(userId)
                .map(BrokerCredential::getStatus)
                .orElse("DISCONNECTED");
        return Map.of("status", status);
    }
}
