package com.cqrs.domain.exception;

public class ConcurrencyException extends RuntimeException {
    public ConcurrencyException(String streamId) {
        super("Optimistic locking conflict on stream: " + streamId);
    }
}
