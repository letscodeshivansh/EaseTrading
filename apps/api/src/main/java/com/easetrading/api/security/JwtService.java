package com.easetrading.api.security;

import com.easetrading.api.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and validates JWT session tokens.
 *
 * A JWT is a signed string the browser sends on every request (in the
 * Authorization header). Because it is signed with our secret, the server can
 * trust its contents without looking anything up in the database — this is what
 * makes the API "stateless" and easy to scale horizontally later.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiryMs;

    public JwtService(AppProperties props) {
        // The secret must be at least 32 bytes for HS256.
        this.signingKey = Keys.hmacShaKeyFor(
                props.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8));
        this.expiryMs = props.getSecurity().getJwtExpiryMinutes() * 60_000;
    }

    /** Issues a token whose "subject" is the user id. */
    public String issueToken(String userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(signingKey)
                .compact();
    }

    /** Returns the user id from a valid token, or throws if invalid/expired. */
    public String extractUserId(String token) {
        return parse(token).getSubject();
    }

    public String extractRole(String token) {
        return parse(token).get("role", String.class);
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
