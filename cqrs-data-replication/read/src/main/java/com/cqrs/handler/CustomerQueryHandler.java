package com.cqrs.handler;

import com.cqrs.dto.CustomerDetailResponse;
import com.cqrs.dto.TransactionDto;
import com.cqrs.entity.CustomerViewEntity;
import com.cqrs.entity.TransactionEntity;
import com.cqrs.entity.WalletEntity;
import com.cqrs.repository.AccountManagerRepository;
import com.cqrs.repository.CustomerViewRepository;
import com.cqrs.repository.TransactionRepository;
import com.cqrs.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerQueryHandler {

    private final CustomerViewRepository customerViewRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public CustomerDetailResponse getCustomerDetail(UUID customerId) {
        CustomerViewEntity view = customerViewRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        List<UUID> walletIds = walletRepository.findAllByCustomerId(customerId)
                .stream().map(WalletEntity::getId).toList();

        List<TransactionDto> transactions = transactionRepository
                .findAllByWalletIdInOrderByOccurredAtDesc(walletIds)
                .stream()
                .map(t -> new TransactionDto(
                        t.getId(), t.getWalletId(), t.getValue(),
                        t.getType(), t.getExchangeConfirmationId(), t.getOccurredAt()))
                .toList();

        return toDetail(view, transactions);
    }

    public List<CustomerViewEntity> listByManager(UUID managerId, BigDecimal minBalance,
                                                   Integer maxDaysInactive, String orderBy,
                                                   int page, int size) {
        return customerViewRepository.findByManager(
                managerId, minBalance, maxDaysInactive, orderBy, size, page * size);
    }

    public List<CustomerViewEntity> listAll(UUID requesterId, BigDecimal minBalance,
                                             Integer maxDaysInactive, String orderBy,
                                             int page, int size) {
        boolean isAdmin = accountManagerRepository.findById(requesterId)
                .map(am -> am.isAdmin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Account manager not found"));

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin profile required");
        }

        return customerViewRepository.findAll(minBalance, maxDaysInactive, orderBy, size, page * size);
    }

    private CustomerDetailResponse toDetail(CustomerViewEntity v, List<TransactionDto> transactions) {
        return new CustomerDetailResponse(
                v.getCustomerId(), v.getCustomerName(), v.getCustomerAge(), v.getCustomerAddress(),
                v.getAccountManagerId(), v.getManagerName(), v.isManagerIsAdmin(),
                v.getBranchId(), v.getBranchRegion(), v.getTotalBalance(), v.getLastTransactionAt(),
                transactions);
    }
}
