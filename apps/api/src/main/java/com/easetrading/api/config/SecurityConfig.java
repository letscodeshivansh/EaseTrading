package com.easetrading.api.config;

import com.easetrading.api.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
 *  - Public endpoints: login/register, health, the live stream, and (for Prompt 1
 *    convenience) the read-only market-data endpoints so the dashboard works
 *    immediately. These will be tightened to require auth in later prompts.
 *  - Everything else requires a valid JWT.
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
            // CSRF protection is for cookie-based sessions; we use bearer tokens, so disable it.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/health",
                    "/api/stream/**",
                    "/api/instruments/**",
                    "/api/quotes/**",
                    "/api/candles/**",
                    "/api/indicators/**",
                    "/api/fundamentals/**",
                    "/api/screener/**",
                    "/api/analysis/**",
                    // Order/portfolio/alert endpoints resolve a dev user when no JWT is
                    // present (single-user dev). In production, remove these from the
                    // permit list so they require authentication.
                    "/api/orders/**",
                    "/api/portfolio/**",
                    "/api/alerts/**"
                ).permitAll()
                .anyRequest().authenticated())
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

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }
}
