package com.eventhub.catalog_service;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for integration tests.
 *
 * <p>Starts a single PostgreSQL container for the whole test JVM (singleton container
 * pattern) and points Spring's datasource at it. Flyway then runs every migration on a
 * clean database, so integration tests exercise the real schema and seed data instead of
 * an in-memory substitute.
 *
 * <p>The container is intentionally never stopped: Testcontainers' Ryuk sidecar removes it
 * when the JVM exits, and reusing one instance keeps the suite fast.
 */
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("catalog")
            .withUsername("eventhub")
            .withPassword("eventhub");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
