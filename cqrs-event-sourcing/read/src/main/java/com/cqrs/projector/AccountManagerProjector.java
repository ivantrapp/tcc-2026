package com.cqrs.projector;

import com.cqrs.event.AccountManagerCreatedMessage;
import com.cqrs.event.AdminProfileGrantedMessage;
import com.cqrs.event.BranchAssignedMessage;
import com.cqrs.event.BranchCreatedMessage;
import com.cqrs.projection.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Gerentes e filiais. Toda mudanca e repassada aos clientes do gerente, porque a
// VIEW do data replication sempre mostra os dados atuais do gerente e da filial.
@Service
@Transactional
@RequiredArgsConstructor
public class AccountManagerProjector {

    private final AccountManagerViewRepository accountManagerViewRepository;
    private final BranchViewRepository branchViewRepository;
    private final CustomerViewRepository customerViewRepository;
    private final CustomerProjector customerProjector;

    public void on(BranchCreatedMessage event) {
        BranchView view = new BranchView();
        view.setId(event.aggregateId());
        view.setRegion(event.region());
        view.setCreatedAt(event.occurredAt());
        branchViewRepository.save(view);

        // Clientes projetados antes da filial chegar.
        customerViewRepository.findAllByBranchId(event.aggregateId())
                .forEach(customer -> customer.setBranchRegion(event.region()));
    }

    public void on(AccountManagerCreatedMessage event) {
        AccountManagerView view = new AccountManagerView();
        view.setId(event.aggregateId());
        view.setName(event.name());
        view.setBranchId(event.branchId());
        view.setAdmin(event.isAdmin());
        view.setCreatedAt(event.occurredAt());
        accountManagerViewRepository.save(view);

        // Clientes projetados antes do gerente chegar.
        refreshCustomers(event.aggregateId());
    }

    public void on(AdminProfileGrantedMessage event) {
        accountManagerViewRepository.findById(event.aggregateId()).ifPresent(view -> {
            view.setAdmin(true);
            refreshCustomers(view.getId());
        });
    }

    public void on(BranchAssignedMessage event) {
        accountManagerViewRepository.findById(event.aggregateId()).ifPresent(view -> {
            view.setBranchId(event.branchId());
            refreshCustomers(view.getId());
        });
    }

    private void refreshCustomers(UUID accountManagerId) {
        accountManagerViewRepository.flush();
        customerViewRepository.findAllByAccountManagerId(accountManagerId)
                .forEach(customer -> customerProjector.applyManager(customer, accountManagerId));
    }
}
