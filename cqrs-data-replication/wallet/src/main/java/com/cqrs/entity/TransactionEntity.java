package com.cqrs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private UUID exchangeConfirmationId;

    @Column(nullable = false)
    private Instant occurredAt;
}
