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
@Table(name = "transactions")
@Getter
public class TransactionEntity {

    @Id
    private UUID id;

    @Column(name = "wallet_id")
    private UUID walletId;

    private BigDecimal value;

    private String type;

    @Column(name = "exchange_confirmation_id")
    private UUID exchangeConfirmationId;

    @Column(name = "occurred_at")
    private Instant occurredAt;
}
