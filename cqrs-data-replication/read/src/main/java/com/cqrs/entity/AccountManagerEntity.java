package com.cqrs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "account_managers")
@Getter
public class AccountManagerEntity {

    @Id
    private UUID id;

    private String name;

    @Column(name = "is_admin")
    private boolean isAdmin;
}
