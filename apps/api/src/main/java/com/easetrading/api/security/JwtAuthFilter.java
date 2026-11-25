package com.easetrading.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.easetrading.api.user.AuthController;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs once per request. If the request carries a valid login token (the et_session
 * cookie, or an "Authorization: Bearer" header), it loads the user id + role into Spring Security's context so the rest of
 * the app knows who is calling. If there's no token (or it's invalid), the request
 * simply continues unauthenticated and protected endpoints will reject it.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = tokenFrom(request);
        if (token != null) {
            try {
                String userId = jwtService.extractUserId(token);
                String role = jwtService.extractRole(token);

                var auth = new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // Invalid/expired token -> leave the request unauthenticated.
            }
        }
        chain.doFilter(request, response);
    }

    /** The login cookie (browser), or an "Authorization: Bearer" header (scripts/tools). */
    private static String tokenFrom(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie c : request.getCookies()) {
                if (AuthController.COOKIE.equals(c.getName()) && !c.getValue().isBlank()) return c.getValue();
            }
        }
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }
}
