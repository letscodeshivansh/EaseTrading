package com.easetrading.api.user;

import com.easetrading.api.common.ApiException;
import com.easetrading.api.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Checks the "credential" that Google's sign-in button hands the browser.
 *
 * It's a token signed by Google. We accept it only if:
 *   - Google's signature is valid (keys fetched from Google) and it hasn't expired,
 *   - it was issued by Google,
 *   - it was issued for OUR app (audience = our Google client ID),
 *   - Google says the email address is verified.
 * Then the email inside is trusted.
 */
@Component
public class GoogleTokenVerifier {

    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final JwtDecoder decoder =
            NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();

    public GoogleTokenVerifier(AppProperties props) {
        this.clientId = props.getSecurity().getGoogleClientId();
    }

    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank();
    }

    public String clientId() {
        return isEnabled() ? clientId : null;
    }

    /** Returns the verified email address, or throws 401. */
    public String verifiedEmail(String credential) {
        if (!isEnabled()) throw new ApiException(HttpStatus.BAD_REQUEST, "Google sign-in is not set up");
        Jwt jwt;
        try {
            jwt = decoder.decode(credential); // checks signature + expiry
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google sign-in failed");
        }
        String issuer = jwt.getClaimAsString("iss");
        boolean forUs = jwt.getAudience() != null && jwt.getAudience().contains(clientId);
        boolean verified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        String email = jwt.getClaimAsString("email");
        if (!ISSUERS.contains(issuer) || !forUs || !verified || email == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google sign-in failed");
        }
        return email.toLowerCase();
    }
}
