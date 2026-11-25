package com.easetrading.api.account;

import com.easetrading.api.common.ApiException;
import com.easetrading.api.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CredentialCipherTest {

    private static CredentialCipher cipher(byte fill) {
        AppProperties props = new AppProperties();
        byte[] key = new byte[32];
        Arrays.fill(key, fill);
        props.getSecurity().setCredentialAesKey(Base64.getEncoder().encodeToString(key));
        return new CredentialCipher(props);
    }

    @Test
    void roundTripsAndNeverStoresPlainText() {
        CredentialCipher c = cipher((byte) 7);
        byte[] enc = c.encrypt("my-groww-secret");
        assertFalse(new String(enc, StandardCharsets.ISO_8859_1).contains("my-groww-secret"));
        assertEquals("my-groww-secret", c.decrypt(enc));
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        CredentialCipher c = cipher((byte) 7);
        assertFalse(Arrays.equals(c.encrypt("same"), c.encrypt("same")));
    }

    @Test
    void tamperedValueIsRejected() {
        CredentialCipher c = cipher((byte) 7);
        byte[] enc = c.encrypt("my-groww-secret");
        enc[enc.length - 1] ^= 1;
        assertThrows(ApiException.class, () -> c.decrypt(enc));
    }

    @Test
    void differentServerKeyCannotRead() {
        byte[] enc = cipher((byte) 7).encrypt("my-groww-secret");
        ApiException e = assertThrows(ApiException.class, () -> cipher((byte) 8).decrypt(enc));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void noServerKeyRefusesToStore() {
        CredentialCipher c = new CredentialCipher(new AppProperties());
        ApiException e = assertThrows(ApiException.class, () -> c.encrypt("x"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }
}
