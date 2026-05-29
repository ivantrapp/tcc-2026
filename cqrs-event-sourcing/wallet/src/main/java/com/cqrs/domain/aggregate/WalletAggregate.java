package com.cqrs.domain.aggregate;

import com.cqrs.domain.event.*;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.domain.model.TransactionType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class WalletAggregate {

    private UUID id;
    private UUID customerId;
    private BigDecimal balance;
    private Instant createdAt;
    private int version = -1;

    private final List<DomainEvent> pendingEvents = new ArrayList<>();

    public static WalletAggregate empty() {
        return new WalletAggregate();
    }

    public static WalletAggregate fromSnapshot(WalletStateSnapshot snap) {
        WalletAggregate wallet = new WalletAggregate();
        wallet.id = snap.id();
        wallet.customerId = snap.customerId();
        wallet.balance = snap.balance();
        wallet.createdAt = snap.createdAt();
        wallet.version = snap.version();
        return wallet;
    }

    public boolean exists() {
        return id != null;
    }

    public void create(UUID walletId, UUID customerId, EventMetadata metadata) {
        WalletCreatedEvent event = WalletCreatedEvent.create(walletId, version + 1, metadata, customerId);
        applyStoredEvent(event);
        pendingEvents.add(event);
    }

    public void registerTransaction(BigDecimal value, TransactionType type,
                                    UUID exchangeConfirmationId, EventMetadata metadata) {
//        validateTradingWindow();
        validatePositiveValue(value);

        TransactionRegisteredEvent event = TransactionRegisteredEvent.create(
                id, version + 1, metadata, value, type, exchangeConfirmationId
        );
        applyStoredEvent(event);
        pendingEvents.add(event);
    }

    public void applyStoredEvent(DomainEvent event) {
        if (event instanceof WalletCreatedEvent e) {
            id = e.aggregateId();
            customerId = e.customerId();
            balance = BigDecimal.ZERO;
            createdAt = e.occurredAt();
        } else if (event instanceof TransactionRegisteredEvent e) {
            if (e.type() == TransactionType.DEPOSIT) {
                balance = balance.add(e.value());
            } else {
                balance = balance.subtract(e.value());
            }
        }
        version = event.version();
    }

    public WalletStateSnapshot toSnapshot() {
        return new WalletStateSnapshot(id, customerId, balance, createdAt, version);
    }

    private void validateTradingWindow() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        DayOfWeek day = now.getDayOfWeek();
        int hour = now.getHour();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY || hour < 8 || hour >= 19) {
            throw new DomainException("Transactions are only allowed Monday through Friday, 08:00–19:00.");
        }
    }

    private void validatePositiveValue(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Transaction value must be positive.");
        }
    }
}
