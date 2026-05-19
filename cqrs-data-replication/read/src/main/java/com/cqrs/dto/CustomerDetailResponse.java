package com.cqrs.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerDetailResponse(
        UUID customerId,
        String customerName,
        int customerAge,
        String customerAddress,
        UUID accountManagerId,
        String managerName,
        boolean managerIsAdmin,
        UUID branchId,
        String branchRegion,
        BigDecimal totalBalance,
        Instant lastTransactionAt,
        List<TransactionDto> transactions
) {}
