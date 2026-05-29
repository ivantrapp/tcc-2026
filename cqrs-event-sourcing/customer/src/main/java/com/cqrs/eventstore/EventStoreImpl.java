package com.cqrs.eventstore;

import com.cqrs.domain.event.AccountManagerAssignedEvent;
import com.cqrs.domain.event.CustomerCreatedEvent;
import com.cqrs.domain.event.CustomerDataUpdatedEvent;
import com.cqrs.domain.event.DomainEvent;
import com.cqrs.domain.exception.ConcurrencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventStoreImpl implements EventStore {

    private final EventRecordRepository eventRepo;
    private final SnapshotRepository snapshotRepo;
    private final ObjectMapper objectMapper;

    @Override
    public void append(String streamId, int expectedVersion, List<DomainEvent> events) {
        try {
            for (DomainEvent event : events) {
                EventRecord record = EventRecord.builder()
                        .streamId(streamId)
                        .version(event.version())
                        .eventType(event.eventType())
                        .payload(serialize(event))
                        .occurredAt(event.occurredAt())
                        .storedAt(Instant.now())
                        .build();
                eventRepo.saveAndFlush(record);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ConcurrencyException(streamId);
        }
    }

    @Override
    public List<DomainEvent> loadStream(String streamId) {
        return eventRepo.findByStreamIdOrderByVersion(streamId)
                .stream()
                .map(this::deserialize)
                .toList();
    }

    @Override
    public List<DomainEvent> loadStream(String streamId, int fromVersion) {
        return eventRepo.findByStreamIdAndVersionGreaterThanEqualOrderByVersion(streamId, fromVersion)
                .stream()
                .map(this::deserialize)
                .toList();
    }

    @Override
    public boolean streamExists(String streamId) {
        return eventRepo.existsByStreamId(streamId);
    }

    @Override
    public Optional<SnapshotRecord> getLatestSnapshot(UUID aggregateId) {
        return snapshotRepo.findTopByAggregateIdOrderByVersionDesc(aggregateId);
    }

    @Override
    public void saveSnapshot(SnapshotRecord snapshot) {
        snapshotRepo.save(snapshot);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize event: " + event.eventType(), e);
        }
    }

    private DomainEvent deserialize(EventRecord record) {
        try {
            return switch (record.getEventType()) {
                case "CustomerCreated"        -> objectMapper.readValue(record.getPayload(), CustomerCreatedEvent.class);
                case "CustomerDataUpdated"    -> objectMapper.readValue(record.getPayload(), CustomerDataUpdatedEvent.class);
                case "AccountManagerAssigned" -> objectMapper.readValue(record.getPayload(), AccountManagerAssignedEvent.class);
                default -> throw new IllegalArgumentException("Unknown event type: " + record.getEventType());
            };
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize event: " + record.getEventType(), e);
        }
    }
}
