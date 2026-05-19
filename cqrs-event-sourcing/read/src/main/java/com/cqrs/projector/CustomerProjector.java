package com.cqrs.projector;

import com.cqrs.event.*;
import com.cqrs.projection.AccountManagerView;
import com.cqrs.projection.AccountManagerViewRepository;
import com.cqrs.projection.CustomerView;
import com.cqrs.projection.CustomerViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerProjector {

    private final CustomerViewRepository customerViewRepository;
    private final AccountManagerViewRepository accountManagerViewRepository;

    public void on(CustomerCreatedMessage event) {
        String amName = accountManagerViewRepository.findById(event.accountManagerId())
                .map(AccountManagerView::getName)
                .orElse(null);

        CustomerView view = new CustomerView();
        view.setId(event.aggregateId());
        view.setName(event.name());
        view.setAddress(event.address());
        view.setAge(event.age());
        view.setAccountManagerId(event.accountManagerId());
        view.setAccountManagerName(amName);
        view.setWalletBalance(BigDecimal.ZERO);
        view.setCreatedAt(event.occurredAt());
        customerViewRepository.save(view);

        accountManagerViewRepository.findById(event.accountManagerId()).ifPresent(am -> {
            am.setCustomerCount(am.getCustomerCount() + 1);
        });
    }

    public void on(CustomerDataUpdatedMessage event) {
        customerViewRepository.findById(event.aggregateId()).ifPresent(view -> {
            if (event.name() != null) view.setName(event.name());
            if (event.address() != null) view.setAddress(event.address());
            if (event.age() != null) view.setAge(event.age());
        });
    }

    public void on(AccountManagerAssignedMessage event) {
        customerViewRepository.findById(event.aggregateId()).ifPresent(view -> {
            accountManagerViewRepository.findById(event.previousAccountManagerId()).ifPresent(prev -> {
                prev.setCustomerCount(Math.max(0, prev.getCustomerCount() - 1));
            });

            String newAmName = accountManagerViewRepository.findById(event.newAccountManagerId())
                    .map(AccountManagerView::getName)
                    .orElse(null);

            view.setAccountManagerId(event.newAccountManagerId());
            view.setAccountManagerName(newAmName);

            accountManagerViewRepository.findById(event.newAccountManagerId()).ifPresent(am -> {
                am.setCustomerCount(am.getCustomerCount() + 1);
            });
        });
    }

    public void on(WalletCreatedMessage event) {
        customerViewRepository.findById(event.customerId())
                .ifPresent(view -> view.setWalletId(event.aggregateId()));
    }

    public void on(TransactionRegisteredMessage event) {
        customerViewRepository.findAllByWalletId(event.aggregateId()).forEach(view -> {
            BigDecimal current = view.getWalletBalance();
            BigDecimal updated = event.type() == TransactionType.DEPOSIT
                    ? current.add(event.value())
                    : current.subtract(event.value());
            view.setWalletBalance(updated);
        });
    }
}
