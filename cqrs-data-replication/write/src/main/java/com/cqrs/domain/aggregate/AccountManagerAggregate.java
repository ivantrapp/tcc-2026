package com.cqrs.domain.aggregate;

import com.cqrs.domain.command.CreateAccountManagerCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.entity.AccountManagerEntity;
import lombok.Getter;

import java.util.UUID;

@Getter
public class AccountManagerAggregate {

    private UUID id;
    private String name;
    private UUID branchId;
    private boolean isAdmin;

    public static AccountManagerAggregate create(CreateAccountManagerCommand cmd) {
        AccountManagerAggregate agg = new AccountManagerAggregate();
        agg.id = UUID.randomUUID();
        agg.name = cmd.name();
        agg.branchId = cmd.branchId();
        agg.isAdmin = cmd.isAdmin();
        return agg;
    }

    public static AccountManagerAggregate from(AccountManagerEntity entity) {
        AccountManagerAggregate agg = new AccountManagerAggregate();
        agg.id = entity.getId();
        agg.name = entity.getName();
        agg.branchId = entity.getBranchId();
        agg.isAdmin = entity.isAdmin();
        return agg;
    }

    public void grantAdmin() {
        if (this.isAdmin) {
            throw new DomainException("Account manager already has admin profile");
        }
        this.isAdmin = true;
    }

    public void assignBranch(UUID branchId) {
        this.branchId = branchId;
    }
}
