package com.cqrs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_managers")
@Getter
@Setter
@NoArgsConstructor
public class AccountManagerEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private UUID branchId;

    @Column(nullable = false)
    private boolean isAdmin;

    @Column(nullable = false)
    private Instant createdAt;
}
