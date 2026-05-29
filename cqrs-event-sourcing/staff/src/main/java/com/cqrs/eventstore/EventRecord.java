package com.cqrs.eventstore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "event_store")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stream_id", nullable = false)
    private String streamId;

    @Column(nullable = false)
    private int version;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "stored_at", nullable = false)
    private Instant storedAt;
}
