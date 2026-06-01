package com.cqrs.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class BranchEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private Instant createdAt;
}
