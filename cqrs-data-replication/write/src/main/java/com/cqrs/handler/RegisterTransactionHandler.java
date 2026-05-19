package com.cqrs.handler;

import com.cqrs.domain.aggregate.WalletAggregate;
import com.cqrs.domain.command.RegisterTransactionCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.entity.TransactionEntity;
import com.cqrs.repository.TransactionRepository;
import com.cqrs.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RegisterTransactionHandler {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public void handle(RegisterTransactionCommand cmd) {
        walletRepository.findById(cmd.walletId())
                .map(walletEntity -> {
                    LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
                    WalletAggregate aggregate = WalletAggregate.from(walletEntity);
                    aggregate.registerTransaction(cmd.value(), cmd.type(), now);

                    walletEntity.setBalance(aggregate.getBalance());

                    TransactionEntity transaction = new TransactionEntity();
                    transaction.setId(UUID.randomUUID());
                    transaction.setWalletId(cmd.walletId());
                    transaction.setValue(cmd.value());
                    transaction.setType(cmd.type().name());
                    transaction.setExchangeConfirmationId(cmd.exchangeConfirmationId());
                    transaction.setOccurredAt(Instant.now());
                    transactionRepository.save(transaction);

                    return walletEntity;
                })
                .orElseThrow(() -> new DomainException("Wallet not found: " + cmd.walletId()));
    }
}
