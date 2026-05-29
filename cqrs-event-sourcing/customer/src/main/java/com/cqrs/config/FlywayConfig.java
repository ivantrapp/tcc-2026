package com.cqrs.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Value("${app.db-schema}")
    private String schema;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .schemas(schema)
                .defaultSchema(schema)
                .createSchemas(true)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        return flyway;
    }
}
