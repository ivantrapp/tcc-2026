package com.cqrs.handler;

import com.cqrs.domain.aggregate.CustomerAggregate;
import com.cqrs.domain.command.AssignAccountManagerCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.repository.AccountManagerRepository;
import com.cqrs.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignAccountManagerHandler {

    private final CustomerRepository customerRepository;
    private final AccountManagerRepository accountManagerRepository;

    public void handle(AssignAccountManagerCommand cmd) {
        if (!accountManagerRepository.existsById(cmd.accountManagerId())) {
            throw new DomainException("Account manager not found: " + cmd.accountManagerId());
        }

        customerRepository.findById(cmd.customerId())
                .map(entity -> {
                    CustomerAggregate aggregate = CustomerAggregate.from(entity);
                    aggregate.assignAccountManager(cmd.accountManagerId());
                    entity.setAccountManagerId(aggregate.getAccountManagerId());
                    return entity;
                })
                .orElseThrow(() -> new DomainException("Customer not found: " + cmd.customerId()));
    }
}
