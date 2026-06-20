package com.easetrading.api.security;

import com.easetrading.api.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM encryption for sensitive data at rest — specifically the Angel One
 * broker credentials. We NEVER store API keys or passwords in plain text.
 *
 * AES-GCM is "authenticated encryption": it both hides the data and detects
 * tampering. Each encryption uses a fresh random 12-byte IV (nonce), which we
 * prepend to the ciphertext so we can decrypt later.
 *
 * The 256-bit key comes from the CREDENTIAL_AES_KEY env var (base64). Generate
 * one with:  openssl rand -base64 32
 */
@Service
public class CryptoService {

    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;        // 96-bit nonce (GCM standard)
    private static final int TAG_LENGTH_BITS = 128; // authentication tag length

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(AppProperties props) {
        SecretKeySpec parsed = tryParseKey(props.getSecurity().getCredentialAesKey());
        if (parsed == null) {
            // No valid key configured (blank or not a proper base64 AES key). Generate
            // an ephemeral one so the app still boots — fine for mock/paper dev. Set a
            // real key (openssl rand -base64 32) before storing real broker credentials.
            byte[] tmp = new byte[32];
            random.nextBytes(tmp);
            parsed = new SecretKeySpec(tmp, "AES");
            System.out.println("[CryptoService] No valid CREDENTIAL_AES_KEY set — using a "
                    + "temporary key for this run. Generate one with: openssl rand -base64 32");
        }
        this.key = parsed;
    }

    /** Parses a base64 AES key (128/192/256-bit), or returns null if invalid. */
    private SecretKeySpec tryParseKey(String b64) {
        if (b64 == null || b64.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(b64.trim());
            if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) return null;
            return new SecretKeySpec(bytes, "AES");
        } catch (IllegalArgumentException e) {
            return null; // not valid base64
        }
    }

    /** Encrypts a UTF-8 string and returns IV+ciphertext as raw bytes. */
    public byte[] encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes());

            // Layout: [12-byte IV][ciphertext+tag]
            return ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv).put(cipherText).array();
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /** Reverses encrypt(): splits off the IV and decrypts the rest. */
    public String decrypt(byte[] ivAndCipher) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(ivAndCipher);
            byte[] iv = new byte[IV_LENGTH];
            buf.get(iv);
            byte[] cipherText = new byte[buf.remaining()];
            buf.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText));
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
