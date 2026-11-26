package com.cqrs.projection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// JSON igual ao AccountManagerEntity do data replication (id, name, admin);
// branchId e createdAt ficam so para a projecao dos clientes.
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

    @JsonIgnore
    private UUID branchId;

    @Column(nullable = false)
    private boolean isAdmin = false;

    @JsonIgnore
    @Column(nullable = false)
    private Instant createdAt;
}
