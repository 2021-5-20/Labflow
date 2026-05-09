package dev.labflow;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "labflow.test.postgres", matches = "true")
class FlywayPostgresMigrationTest {
    @Test
    void migrationsApplyOnPostgresWithPgvector() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
                DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres")
        )) {
            postgres.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load();

            var result = flyway.migrate();

            assertThat(result.success).isTrue();
            assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(3);
        }
    }
}
