package com.cqrs.event;

import java.time.Instant;
import java.util.UUID;

public record AdminProfileGrantedMessage(
        UUID aggregateId,
        Instant occurredAt,
        String grantedBy
) {}
