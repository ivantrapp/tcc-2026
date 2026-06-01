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

    @Value("${replication.user:replicator}")
    private String replicationUser;

    @Value("${replication.password:replicator}")
    private String replicationPassword;

    @Value("${replication.staff.host:localhost}")
    private String staffHost;

    @Value("${replication.staff.port:5457}")
    private int staffPort;

    @Value("${replication.staff.name:crm_staff}")
    private String staffDb;

    @Value("${replication.customer.host:localhost}")
    private String customerHost;

    @Value("${replication.customer.port:5458}")
    private int customerPort;

    @Value("${replication.customer.name:crm_customer}")
    private String customerDb;

    @Value("${replication.wallet.host:localhost}")
    private String walletHost;

    @Value("${replication.wallet.port:5459}")
    private int walletPort;

    @Value("${replication.wallet.name:crm_wallet}")
    private String walletDb;

    @Bean
    @DependsOn("flyway")
    public String walSubscriptions(DataSource dataSource) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);

            createSubscription(conn,
                "crm_staff_subscription",
                connStr(staffHost, staffPort, staffDb),
                "crm_staff_publication");

            createSubscription(conn,
                "crm_customer_subscription",
                connStr(customerHost, customerPort, customerDb),
                "crm_customer_publication");

            createSubscription(conn,
                "crm_wallet_subscription",
                connStr(walletHost, walletPort, walletDb),
                "crm_wallet_publication");
        }
        return "wal-subscriptions-ready";
    }

    private void createSubscription(Connection conn, String name, String connStr, String publication)
            throws Exception {

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT 1 FROM pg_subscription WHERE subname = '" + name + "'")) {
            if (rs.next()) {
                log.info("WAL subscription '{}' já existe — ignorando", name);
                return;
            }
        }

        try (Statement st = conn.createStatement()) {
            st.execute(String.format(
                    "CREATE SUBSCRIPTION %s CONNECTION '%s' PUBLICATION %s",
                    name, connStr, publication));
            log.info("WAL subscription '{}' criada", name);
        }
    }

    private String connStr(String host, int port, String db) {
        return String.format("host=%s port=%d dbname=%s user=%s password=%s",
                host, port, db, replicationUser, replicationPassword);
    }
}
