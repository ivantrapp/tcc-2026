package com.cqrs.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountManagerViewRepository extends JpaRepository<AccountManagerView, UUID> {}
