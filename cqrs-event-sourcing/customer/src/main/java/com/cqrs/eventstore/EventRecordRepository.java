package com.cqrs.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRecordRepository extends JpaRepository<EventRecord, Long> {

    List<EventRecord> findByStreamIdOrderByVersion(String streamId);

    List<EventRecord> findByStreamIdAndVersionGreaterThanEqualOrderByVersion(String streamId, int fromVersion);

    boolean existsByStreamId(String streamId);
}
