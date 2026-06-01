package com.cqrs.handler;

import com.cqrs.domain.aggregate.AccountManagerAggregate;
import com.cqrs.domain.command.GrantAdminProfileCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.repository.AccountManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GrantAdminProfileHandler {

    private final AccountManagerRepository accountManagerRepository;

    public void handle(GrantAdminProfileCommand cmd) {
        accountManagerRepository.findById(cmd.accountManagerId())
                .map(entity -> {
                    AccountManagerAggregate aggregate = AccountManagerAggregate.from(entity);
                    aggregate.grantAdmin();
                    entity.setAdmin(aggregate.isAdmin());
                    return entity;
                })
                .orElseThrow(() -> new DomainException("Account manager not found: " + cmd.accountManagerId()));
    }
}
