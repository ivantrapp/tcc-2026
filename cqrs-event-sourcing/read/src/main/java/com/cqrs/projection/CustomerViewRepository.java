package com.cqrs.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerViewRepository extends JpaRepository<CustomerView, UUID> {

    List<CustomerView> findAllByAccountManagerId(UUID accountManagerId);

    List<CustomerView> findAllByBranchId(UUID branchId);
}
