package com.cqrs.controller;

import com.cqrs.entity.AccountManagerEntity;
import com.cqrs.repository.AccountManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/account-managers")
@RequiredArgsConstructor
public class AccountManagerQueryController {

    private final AccountManagerRepository accountManagerRepository;

    @GetMapping("/{id}")
    public ResponseEntity<AccountManagerEntity> getById(@PathVariable UUID id) {
        return accountManagerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
