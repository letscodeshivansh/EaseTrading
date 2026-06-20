package com.easetrading.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point for the EaseTrading core backend.
 *
 * This is the "system of record": it owns users, broker sessions, orders and
 * market-data ingestion. The frontend talks only to this service; this service
 * talks to Angel One, the analysis service, Postgres and Redis.
 *
 * - @EnableScheduling : lets us run periodic jobs (e.g. daily symbol-master sync).
 * - @EnableAsync      : lets the market-data ingester run on background threads.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
