package com.cqrs.event;

import java.time.Instant;
import java.util.UUID;

public record WalletCreatedMessage(
        UUID aggregateId,
        Instant occurredAt,
        UUID customerId
) {}
