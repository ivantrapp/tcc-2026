package com.cqrs.event;

import java.time.Instant;
import java.util.UUID;

public record AccountManagerAssignedMessage(
        UUID aggregateId,
        Instant occurredAt,
        UUID previousAccountManagerId,
        UUID newAccountManagerId
) {}
