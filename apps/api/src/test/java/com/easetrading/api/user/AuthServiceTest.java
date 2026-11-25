package com.easetrading.api.user;

import com.easetrading.api.common.ApiException;
import com.easetrading.api.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private AuthService auth;
    private final Map<String, User> db = new HashMap<>();

    @BeforeEach
    void setup() {
        UserRepository users = Mockito.mock(UserRepository.class);
        when(users.existsByEmail(anyString())).thenAnswer(i -> db.containsKey(i.<String>getArgument(0)));
        when(users.findByEmail(anyString())).thenAnswer(i -> Optional.ofNullable(db.get(i.<String>getArgument(0))));
        when(users.save(any())).thenAnswer(i -> { User u = i.getArgument(0); db.put(u.getEmail(), u); return u; });
        // No GOOGLE_CLIENT_ID configured.
        auth = new AuthService(users, new BCryptPasswordEncoder(), new GoogleTokenVerifier(new AppProperties()));
    }

    @Test
    void registerThenLoginIgnoringEmailCase() {
        auth.register("Me@Example.com", "secret-pass");
        assertEquals("me@example.com", auth.login("ME@example.com", "secret-pass").getEmail());
    }

    @Test
    void duplicateEmailIsRejected() {
        auth.register("me@example.com", "secret-pass");
        ApiException e = assertThrows(ApiException.class, () -> auth.register("me@example.com", "other-pass"));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    void wrongPasswordIsRejectedAndFiveFailuresLockTheEmail() {
        auth.register("me@example.com", "secret-pass");
        for (int i = 0; i < 5; i++) {
            ApiException e = assertThrows(ApiException.class, () -> auth.login("me@example.com", "wrong"));
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
        }
        // Even the right password is refused while locked.
        ApiException locked = assertThrows(ApiException.class, () -> auth.login("me@example.com", "secret-pass"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, locked.getStatus());
    }

    @Test
    void googleSignInIsOffWithoutAClientId() {
        ApiException e = assertThrows(ApiException.class, () -> auth.loginWithGoogle("any-token"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }
}
