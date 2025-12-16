package me.adilfulara.autoledger;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton PostgreSQL container for integration tests.
 * Shares a single container instance across all test classes.
 */
public final class PostgreSQLTestContainer {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine")
            .asCompatibleSubstituteFor("postgres");

    private static final PostgreSQLContainer INSTANCE;

    static {
        INSTANCE = new PostgreSQLContainer(POSTGRES_IMAGE)
                .withDatabaseName("autoledger_test")
                .withUsername("test")
                .withPassword("test");
        INSTANCE.start();
    }

    private PostgreSQLTestContainer() {
        // Prevent instantiation
    }

    public static PostgreSQLContainer getInstance() {
        return INSTANCE;
    }

    /**
     * Configure Spring datasource properties for integration tests.
     * Call from @DynamicPropertySource in test classes.
     */
    public static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", INSTANCE::getUsername);
        registry.add("spring.datasource.password", INSTANCE::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.schema", () -> "app");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.schemas", () -> "app");
        registry.add("spring.flyway.default-schema", () -> "app");
    }
}
