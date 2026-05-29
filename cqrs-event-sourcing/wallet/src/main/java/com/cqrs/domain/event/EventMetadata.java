package com.cqrs.domain.event;

import java.util.UUID;

public record EventMetadata(UUID correlationId, String causedBy) {}
