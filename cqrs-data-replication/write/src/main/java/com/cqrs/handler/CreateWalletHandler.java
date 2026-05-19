package com.cqrs.handler;

import com.cqrs.domain.aggregate.WalletAggregate;
import com.cqrs.domain.command.CreateWalletCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.entity.WalletEntity;
import com.cqrs.repository.CustomerRepository;
import com.cqrs.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateWalletHandler {

    private final WalletRepository walletRepository;
    private final CustomerRepository customerRepository;

    public UUID handle(CreateWalletCommand cmd) {
        if (!customerRepository.existsById(cmd.customerId())) {
            throw new DomainException("Customer not found: " + cmd.customerId());
        }

        WalletAggregate aggregate = WalletAggregate.create(cmd.customerId());

        WalletEntity entity = new WalletEntity();
        entity.setId(aggregate.getId());
        entity.setCustomerId(aggregate.getCustomerId());
        entity.setBalance(aggregate.getBalance());
        entity.setCreatedAt(Instant.now());
        walletRepository.save(entity);

        return entity.getId();
    }
}
