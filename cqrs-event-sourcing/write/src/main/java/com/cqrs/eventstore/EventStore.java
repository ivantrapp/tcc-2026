package com.cqrs.eventstore;

import com.cqrs.domain.event.DomainEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventStore {

    void append(String streamId, int expectedVersion, List<DomainEvent> events);

    List<DomainEvent> loadStream(String streamId);

    List<DomainEvent> loadStream(String streamId, int fromVersion);

    boolean streamExists(String streamId);

    Optional<SnapshotRecord> getLatestSnapshot(UUID aggregateId);

    void saveSnapshot(SnapshotRecord snapshot);
}
