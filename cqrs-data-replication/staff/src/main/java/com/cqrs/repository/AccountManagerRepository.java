package com.cqrs.repository;

import com.cqrs.entity.AccountManagerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountManagerRepository extends JpaRepository<AccountManagerEntity, UUID> {}
