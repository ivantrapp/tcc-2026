package com.cqrs.projector;

import com.cqrs.event.*;
import com.cqrs.projection.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

// Mantem customer_view com as mesmas regras da VIEW do data replication:
// dados atuais do gerente e da filial, total_balance = soma das carteiras do
// cliente e last_transaction_at = data da ultima transacao entre elas.
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerProjector {

    private final CustomerViewRepository customerViewRepository;
    private final AccountManagerViewRepository accountManagerViewRepository;
    private final BranchViewRepository branchViewRepository;
    private final WalletViewRepository walletViewRepository;

    public void on(CustomerCreatedMessage event) {
        CustomerView view = new CustomerView();
        view.setCustomerId(event.aggregateId());
        view.setCustomerName(event.name());
        view.setCustomerAddress(event.address());
        view.setCustomerAge(event.age());
        applyManager(view, event.accountManagerId());
        customerViewRepository.save(view);
    }

    public void on(CustomerDataUpdatedMessage event) {
        customerViewRepository.findById(event.aggregateId()).ifPresent(view -> {
            if (event.name() != null) view.setCustomerName(event.name());
            if (event.address() != null) view.setCustomerAddress(event.address());
            if (event.age() != null) view.setCustomerAge(event.age());
        });
    }

    public void on(AccountManagerAssignedMessage event) {
        customerViewRepository.findById(event.aggregateId())
                .ifPresent(view -> applyManager(view, event.newAccountManagerId()));
    }

    public void on(WalletCreatedMessage event) {
        WalletView wallet = new WalletView();
        wallet.setId(event.aggregateId());
        wallet.setCustomerId(event.customerId());
        walletViewRepository.save(wallet);
    }

    public void on(TransactionRegisteredMessage event) {
        walletViewRepository.findById(event.aggregateId()).ifPresent(wallet -> {
            BigDecimal delta = event.type() == TransactionType.DEPOSIT ? event.value() : event.value().negate();
            wallet.setBalance(wallet.getBalance().add(delta));

            customerViewRepository.findById(wallet.getCustomerId()).ifPresent(view -> {
                view.setTotalBalance(view.getTotalBalance().add(delta));
                if (view.getLastTransactionAt() == null || event.occurredAt().isAfter(view.getLastTransactionAt())) {
                    view.setLastTransactionAt(event.occurredAt());
                }
            });
        });
    }

    // Copia para o cliente os dados atuais do gerente e da filial (LEFT JOIN da VIEW do DR).
    void applyManager(CustomerView view, UUID accountManagerId) {
        view.setAccountManagerId(accountManagerId);
        AccountManagerView manager = accountManagerId == null ? null
                : accountManagerViewRepository.findById(accountManagerId).orElse(null);
        if (manager == null) {
            view.setManagerName(null);
            view.setManagerIsAdmin(false);
            view.setBranchId(null);
            view.setBranchRegion(null);
            return;
        }
        view.setManagerName(manager.getName());
        view.setManagerIsAdmin(manager.isAdmin());
        view.setBranchId(manager.getBranchId());
        view.setBranchRegion(manager.getBranchId() == null ? null
                : branchViewRepository.findById(manager.getBranchId()).map(BranchView::getRegion).orElse(null));
    }
}
