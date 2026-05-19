package com.cqrs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "customer_view")
@Getter
public class CustomerViewEntity {

    @Id
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_age")
    private int customerAge;

    @Column(name = "customer_address")
    private String customerAddress;

    @Column(name = "account_manager_id")
    private UUID accountManagerId;

    @Column(name = "manager_name")
    private String managerName;

    @Column(name = "manager_is_admin")
    private boolean managerIsAdmin;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "branch_region")
    private String branchRegion;

    @Column(name = "total_balance")
    private BigDecimal totalBalance;

    @Column(name = "last_transaction_at")
    private Instant lastTransactionAt;
}
