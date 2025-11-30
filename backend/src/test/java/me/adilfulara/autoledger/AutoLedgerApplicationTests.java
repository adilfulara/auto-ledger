package me.adilfulara.autoledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Basic application context test.
 * Verifies that the Spring Boot application starts correctly with all configurations.
 * Uses H2 in-memory database configured in application.yml.
 */
@SpringBootTest
class AutoLedgerApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures the Spring context loads successfully with Flyway migrations
    }

}
