package com.cqrs.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SnapshotRepository extends JpaRepository<SnapshotRecord, Long> {

    Optional<SnapshotRecord> findTopByAggregateIdOrderByVersionDesc(UUID aggregateId);
}
