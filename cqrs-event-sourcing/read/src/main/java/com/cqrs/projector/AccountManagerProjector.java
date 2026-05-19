package com.cqrs.projector;

import com.cqrs.event.AccountManagerCreatedMessage;
import com.cqrs.event.AdminProfileGrantedMessage;
import com.cqrs.event.BranchAssignedMessage;
import com.cqrs.projection.AccountManagerView;
import com.cqrs.projection.AccountManagerViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountManagerProjector {

    private final AccountManagerViewRepository accountManagerViewRepository;

    public void on(AccountManagerCreatedMessage event) {
        AccountManagerView view = new AccountManagerView();
        view.setId(event.aggregateId());
        view.setName(event.name());
        view.setBranchId(event.branchId());
        view.setAdmin(event.isAdmin());
        view.setCustomerCount(0);
        view.setCreatedAt(event.occurredAt());
        accountManagerViewRepository.save(view);
    }

    public void on(AdminProfileGrantedMessage event) {
        accountManagerViewRepository.findById(event.aggregateId())
                .ifPresent(view -> view.setAdmin(true));
    }

    public void on(BranchAssignedMessage event) {
        accountManagerViewRepository.findById(event.aggregateId())
                .ifPresent(view -> view.setBranchId(event.branchId()));
    }
}
