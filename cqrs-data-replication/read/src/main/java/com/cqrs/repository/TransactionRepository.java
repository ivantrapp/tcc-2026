package com.cqrs.repository;

import com.cqrs.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    List<TransactionEntity> findAllByWalletIdInOrderByOccurredAtDesc(List<UUID> walletIds);
}
