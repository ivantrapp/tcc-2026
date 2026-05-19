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

@Entity
@Table(name = "customer_view")
@Getter
@Setter
@NoArgsConstructor
public class CustomerView {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String address;

    private Integer age;

    private UUID accountManagerId;

    private String accountManagerName;

    private UUID walletId;

    @Column(nullable = false)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant createdAt;
}
