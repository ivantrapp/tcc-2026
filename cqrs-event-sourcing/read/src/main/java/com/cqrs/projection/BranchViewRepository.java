package com.cqrs.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BranchViewRepository extends JpaRepository<BranchView, UUID> {
}
