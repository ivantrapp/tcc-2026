package com.cqrs.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class SubscriptionConfig {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionConfig.class);

    // Host/port/db of the WRITE database as seen from inside the Docker network.
    // Overridden via environment variables in docker-compose; defaults work for local dev.
    @Value("${replication.write.host:localhost}")
    private String writeHost;

    @Value("${replication.write.port:5456}")
    private int writePort;

    @Value("${replication.write.name:crm_write}")
    private String writeDb;

    @Value("${replication.user:replicator}")
    private String replicationUser;

    @Value("${replication.password:replicator}")
    private String replicationPassword;

    @Value("${replication.publication:crm_publication}")
    private String publication;

    @Value("${replication.subscription:crm_subscription}")
    private String subscription;

    // DependsOn ensures Flyway has created all tables before we attach the subscription
    @Bean
    @DependsOn("flyway")
    public String walSubscription(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            // CREATE SUBSCRIPTION cannot run inside a transaction block
            conn.setAutoCommit(true);

            try (Statement st = conn.createStatement()) {
                // Idempotent: skip if subscription already exists
                try (ResultSet rs = st.executeQuery(
                        "SELECT 1 FROM pg_subscription WHERE subname = '" + subscription + "'")) {
                    if (rs.next()) {
                        log.info("WAL subscription '{}' already exists — skipping", subscription);
                        return subscription;
                    }
                }

                String connInfo = String.format(
                        "host=%s port=%d dbname=%s user=%s password=%s",
                        writeHost, writePort, writeDb, replicationUser, replicationPassword);

                st.execute(String.format(
                        "CREATE SUBSCRIPTION %s CONNECTION '%s' PUBLICATION %s",
                        subscription, connInfo, publication));

                log.info("WAL subscription '{}' created successfully", subscription);
            }
        } catch (Exception e) {
            log.error("Failed to create WAL subscription '{}': {}", subscription, e.getMessage(), e);
            throw new RuntimeException("WAL subscription setup failed", e);
        }
        return subscription;
    }
}
