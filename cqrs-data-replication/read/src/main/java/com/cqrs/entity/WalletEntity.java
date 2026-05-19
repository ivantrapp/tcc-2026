package com.cqrs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "wallets")
@Getter
public class WalletEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    private BigDecimal balance;
}
