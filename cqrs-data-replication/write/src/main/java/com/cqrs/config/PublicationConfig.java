package com.cqrs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Configuration
public class PublicationConfig {

    private static final Logger log = LoggerFactory.getLogger(PublicationConfig.class);

    private static final String PUBLICATION = "crm_publication";
    private static final String TABLES = "branches, account_managers, customers, wallets, transactions";

    @Bean
    @DependsOn("flyway")
    public String walPublication(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);
            try (Statement st = conn.createStatement()) {
                try (ResultSet rs = st.executeQuery(
                        "SELECT 1 FROM pg_publication WHERE pubname = '" + PUBLICATION + "'")) {
                    if (rs.next()) {
                        log.info("WAL publication '{}' already exists — skipping", PUBLICATION);
                        return PUBLICATION;
                    }
                }
                st.execute("CREATE PUBLICATION " + PUBLICATION + " FOR TABLE " + TABLES);
                log.info("WAL publication '{}' created for tables: {}", PUBLICATION, TABLES);
            }
        } catch (Exception e) {
            log.error("Failed to create WAL publication '{}': {}", PUBLICATION, e.getMessage(), e);
            throw new RuntimeException("WAL publication setup failed", e);
        }
        return PUBLICATION;
    }
}
