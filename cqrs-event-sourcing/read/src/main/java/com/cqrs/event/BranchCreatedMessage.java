package com.cqrs.event;

import java.time.Instant;
import java.util.UUID;

public record BranchCreatedMessage(
        UUID aggregateId,
        Instant occurredAt,
        String region
) {}
