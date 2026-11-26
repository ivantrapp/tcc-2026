package com.cqrs.controller;

import com.cqrs.entity.CustomerViewEntity;
import com.cqrs.repository.CustomerViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Mesmas consultas do read do event sourcing (CustomerViewController), para que o
// teste de leitura compare as arquiteturas com a mesma carga de consulta.
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerQueryController {

    private final CustomerViewRepository customerViewRepository;

    @GetMapping
    public List<CustomerViewEntity> listAll() {
        return customerViewRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerViewEntity> findById(@PathVariable UUID id) {
        return customerViewRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-account-manager/{accountManagerId}")
    public List<CustomerViewEntity> findByAccountManager(@PathVariable UUID accountManagerId) {
        return customerViewRepository.findAllByAccountManagerId(accountManagerId);
    }
}
