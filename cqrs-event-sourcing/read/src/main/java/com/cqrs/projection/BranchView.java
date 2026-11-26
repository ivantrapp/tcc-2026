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
@Table(name = "branch_view")
@Getter
@Setter
@NoArgsConstructor
public class BranchView {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private Instant createdAt;
}
