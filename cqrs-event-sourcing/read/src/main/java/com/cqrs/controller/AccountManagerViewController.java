package com.cqrs.controller;

import com.cqrs.projection.AccountManagerView;
import com.cqrs.projection.AccountManagerViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/account-managers")
@RequiredArgsConstructor
public class AccountManagerViewController {

    private final AccountManagerViewRepository accountManagerViewRepository;

    @GetMapping("/{id}")
    public ResponseEntity<AccountManagerView> findById(@PathVariable UUID id) {
        return accountManagerViewRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
