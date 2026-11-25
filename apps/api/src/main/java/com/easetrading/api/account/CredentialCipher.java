package com.easetrading.api.account;

import com.easetrading.api.common.ApiException;
import com.easetrading.api.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts users' Groww keys before they reach the database (AES-256-GCM).
 *
 * The key comes from CREDENTIAL_AES_KEY (base64 of 32 bytes). Every value gets a
 * fresh random 12-byte IV, stored in front of the ciphertext. GCM also detects
 * tampering: a modified value fails to decrypt instead of returning garbage.
 *
 * No key configured = saving Groww keys is refused (we never fall back to a
 * temporary key, which would make saved keys unreadable after a restart).
 */
@Component
public class CredentialCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(AppProperties props) {
        this.key = parse(props.getSecurity().getCredentialAesKey());
    }

    public byte[] encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("encryption failed", e);
        }
    }

    public String decrypt(byte[] ivAndCiphertext) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(ivAndCiphertext);
            byte[] iv = new byte[IV_BYTES];
            buf.get(iv);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Saved Groww keys can't be read (the server key changed) — please enter them again in Settings");
        }
    }

    private SecretKeySpec requireKey() {
        if (key == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The server has no CREDENTIAL_AES_KEY, so it can't store Groww keys safely");
        }
        return key;
    }

    private static SecretKeySpec parse(String b64) {
        if (b64 == null || b64.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(b64.trim());
            return bytes.length == 32 ? new SecretKeySpec(bytes, "AES") : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
