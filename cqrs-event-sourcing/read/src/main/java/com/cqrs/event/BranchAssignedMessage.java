package com.cqrs.event;

import java.time.Instant;
import java.util.UUID;

public record BranchAssignedMessage(
        UUID aggregateId,
        Instant occurredAt,
        UUID branchId
) {}
