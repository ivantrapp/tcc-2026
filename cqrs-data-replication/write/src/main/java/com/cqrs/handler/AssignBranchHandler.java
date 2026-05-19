package com.cqrs.handler;

import com.cqrs.domain.aggregate.AccountManagerAggregate;
import com.cqrs.domain.command.AssignBranchCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.repository.AccountManagerRepository;
import com.cqrs.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignBranchHandler {

    private final AccountManagerRepository accountManagerRepository;
    private final BranchRepository branchRepository;

    public void handle(AssignBranchCommand cmd) {
        if (!branchRepository.existsById(cmd.branchId())) {
            throw new DomainException("Branch not found: " + cmd.branchId());
        }

        accountManagerRepository.findById(cmd.accountManagerId())
                .map(entity -> {
                    AccountManagerAggregate aggregate = AccountManagerAggregate.from(entity);
                    aggregate.assignBranch(cmd.branchId());
                    entity.setBranchId(aggregate.getBranchId());
                    return entity;
                })
                .orElseThrow(() -> new DomainException("Account manager not found: " + cmd.accountManagerId()));
    }
}
