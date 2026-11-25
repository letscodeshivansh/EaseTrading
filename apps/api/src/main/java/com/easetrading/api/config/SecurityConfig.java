package com.easetrading.api.config;

import com.easetrading.api.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central security rules for the API.
 *
 *  - Stateless sessions (no server-side session; the JWT carries identity).
 *  - Public: only login/register/Google sign-in and the health check.
 *  - Everything else (portfolio, watchlist, alerts, prices, live stream…) needs login.
 *  - Passwords are hashed with BCrypt (never stored in plain text).
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // Comma-separated list of allowed browser origins. Defaults cover local dev and
    // any Vercel deployment; override with CORS_ALLOWED_ORIGINS in production.
    @org.springframework.beans.factory.annotation.Value(
            "${CORS_ALLOWED_ORIGINS:http://localhost:3000,https://*.vercel.app}")
    private String corsOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsSource()))
            // The login cookie is SameSite=Lax, so other websites can't send it with their
            // POST/DELETE requests; together with the strict CORS list below, that covers CSRF.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Everything needs a logged-in user except the login endpoints and the health check.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/health").permitAll()
                .anyRequest().authenticated())
            // Not logged in -> plain 401 (the web app then shows the login page).
            .exceptionHandling(e -> e.authenticationEntryPoint(
                    new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                            org.springframework.http.HttpStatus.UNAUTHORIZED)))
            // Our JWT filter runs before Spring's username/password filter.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Allow the configured frontend origins (localhost + Vercel by default) to call this API. */
    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Patterns (not exact origins) so wildcards like https://*.vercel.app work,
        // which also covers Vercel's per-deployment preview URLs.
        cfg.setAllowedOriginPatterns(List.of(corsOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
