package com.cqrs.domain.aggregate;

import com.cqrs.domain.exception.DomainException;
import com.cqrs.domain.model.TransactionType;
import com.cqrs.entity.WalletEntity;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class WalletAggregate {

    private UUID id;
    private UUID customerId;
    private BigDecimal balance;

    public static WalletAggregate create(UUID customerId) {
        WalletAggregate agg = new WalletAggregate();
        agg.id = UUID.randomUUID();
        agg.customerId = customerId;
        agg.balance = BigDecimal.ZERO;
        return agg;
    }

    public static WalletAggregate from(WalletEntity entity) {
        WalletAggregate agg = new WalletAggregate();
        agg.id = entity.getId();
        agg.customerId = entity.getCustomerId();
        agg.balance = entity.getBalance();
        return agg;
    }

    public void registerTransaction(BigDecimal value, TransactionType type, LocalDateTime at) {
        //validateTimeWindow(at);
        if (type == TransactionType.DEPOSIT) {
            balance = balance.add(value);
        } else {
            balance = balance.subtract(value);
        }
    }

    private void validateTimeWindow(LocalDateTime at) {
        DayOfWeek day = at.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            throw new DomainException("Transactions are not allowed on weekends");
        }
        int hour = at.getHour();
        if (hour < 8 || hour >= 19) {
            throw new DomainException("Transactions are only allowed between 08:00 and 19:00");
        }
    }
}
