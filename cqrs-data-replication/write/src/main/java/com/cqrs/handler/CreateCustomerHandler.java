package com.cqrs.handler;

import com.cqrs.domain.aggregate.CustomerAggregate;
import com.cqrs.domain.command.CreateCustomerCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.entity.CustomerEntity;
import com.cqrs.repository.AccountManagerRepository;
import com.cqrs.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateCustomerHandler {

    private final CustomerRepository customerRepository;
    private final AccountManagerRepository accountManagerRepository;

    public UUID handle(CreateCustomerCommand cmd) {
        if (!accountManagerRepository.existsById(cmd.accountManagerId())) {
            throw new DomainException("Account manager not found: " + cmd.accountManagerId());
        }

        CustomerAggregate aggregate = CustomerAggregate.create(cmd);

        CustomerEntity entity = new CustomerEntity();
        entity.setId(aggregate.getId());
        entity.setName(aggregate.getName());
        entity.setAddress(aggregate.getAddress());
        entity.setAge(aggregate.getAge());
        entity.setAccountManagerId(aggregate.getAccountManagerId());
        entity.setCreatedAt(Instant.now());
        customerRepository.save(entity);

        return entity.getId();
    }
}
