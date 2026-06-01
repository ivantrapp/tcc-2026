package com.cqrs.repository;

import com.cqrs.entity.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BranchRepository extends JpaRepository<BranchEntity, UUID> {}
