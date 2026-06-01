package com.cqrs.handler;

import com.cqrs.domain.aggregate.AccountManagerAggregate;
import com.cqrs.domain.command.CreateAccountManagerCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.entity.AccountManagerEntity;
import com.cqrs.repository.AccountManagerRepository;
import com.cqrs.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateAccountManagerHandler {

    private final AccountManagerRepository accountManagerRepository;
    private final BranchRepository branchRepository;

    public UUID handle(CreateAccountManagerCommand cmd) {
        if (!branchRepository.existsById(cmd.branchId())) {
            throw new DomainException("Branch not found: " + cmd.branchId());
        }

        AccountManagerAggregate aggregate = AccountManagerAggregate.create(cmd);

        AccountManagerEntity entity = new AccountManagerEntity();
        entity.setId(aggregate.getId());
        entity.setName(aggregate.getName());
        entity.setBranchId(aggregate.getBranchId());
        entity.setAdmin(aggregate.isAdmin());
        entity.setCreatedAt(Instant.now());
        accountManagerRepository.save(entity);

        return entity.getId();
    }
}
