package com.cqrs.event;

import java.time.Instant;
import java.util.UUID;

public record AccountManagerCreatedMessage(
        UUID aggregateId,
        Instant occurredAt,
        String name,
        UUID branchId,
        boolean isAdmin
) {}
