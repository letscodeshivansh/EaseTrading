package com.easetrading.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point for the EaseTrading core backend.
 *
 * Owns users and login, the broker session and market data. The frontend talks
 * only to this service; this service talks to the broker (Groww), the Python
 * analysis service and its SQLite file.
 *
 * - @EnableScheduling : periodic jobs (daily stock-list sync, price polling, alerts).
 * - Spring's built-in "generated password" user is switched off: we have our own
 *   login (users table + login cookie), so it would only be an unused extra account.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@EnableAsync
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
