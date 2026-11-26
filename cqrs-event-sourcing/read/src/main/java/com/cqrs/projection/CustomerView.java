package com.cqrs.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Mesmos campos (e mesmo JSON) do CustomerViewEntity do data replication.
@Entity
@Table(name = "customer_view")
@Getter
@Setter
@NoArgsConstructor
public class CustomerView {

    @Id
    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_age", nullable = false)
    private int customerAge;

    @Column(name = "customer_address")
    private String customerAddress;

    @Column(name = "account_manager_id")
    private UUID accountManagerId;

    @Column(name = "manager_name")
    private String managerName;

    @Column(name = "manager_is_admin", nullable = false)
    private boolean managerIsAdmin;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "branch_region")
    private String branchRegion;

    @Column(name = "total_balance", nullable = false)
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @Column(name = "last_transaction_at")
    private Instant lastTransactionAt;
}
