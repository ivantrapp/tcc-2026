package com.cqrs.domain.aggregate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletStateSnapshot(UUID id, UUID customerId, BigDecimal balance, Instant createdAt, int version) {}
