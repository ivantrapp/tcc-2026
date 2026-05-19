package com.cqrs.projection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_manager_view")
@Getter
@Setter
@NoArgsConstructor
public class AccountManagerView {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private UUID branchId;

    @Column(nullable = false)
    private boolean isAdmin = false;

    @Column(nullable = false)
    private int customerCount = 0;

    @Column(nullable = false)
    private Instant createdAt;
}
