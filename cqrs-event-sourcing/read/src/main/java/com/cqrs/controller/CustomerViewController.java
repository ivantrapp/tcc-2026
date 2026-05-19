package com.cqrs.controller;

import com.cqrs.projection.CustomerView;
import com.cqrs.projection.CustomerViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerViewController {

    private final CustomerViewRepository customerViewRepository;

    @GetMapping
    public List<CustomerView> listAll() {
        return customerViewRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerView> findById(@PathVariable UUID id) {
        return customerViewRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-account-manager/{accountManagerId}")
    public List<CustomerView> findByAccountManager(@PathVariable UUID accountManagerId) {
        return customerViewRepository.findAllByAccountManagerId(accountManagerId);
    }
}
