package com.cqrs.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

// Carteiras do cliente: o saldo total da customer_view e a soma delas, como no data replication.
@Entity
@Table(name = "wallet_view")
@Getter
@Setter
@NoArgsConstructor
public class WalletView {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;
}
