package com.cqrs.controller;

import com.cqrs.dto.CustomerDetailResponse;
import com.cqrs.entity.CustomerViewEntity;
import com.cqrs.handler.CustomerQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerQueryController {

    private final CustomerQueryHandler queryHandler;

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailResponse> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(queryHandler.getCustomerDetail(id));
    }

    @GetMapping("/by-manager/{managerId}")
    public ResponseEntity<List<CustomerViewEntity>> listByManager(
            @PathVariable UUID managerId,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) Integer maxDaysInactive,
            @RequestParam(defaultValue = "totalBalance") String orderBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                queryHandler.listByManager(managerId, minBalance, maxDaysInactive, orderBy, page, size));
    }

    @GetMapping
    public ResponseEntity<List<CustomerViewEntity>> listAll(
            @RequestParam UUID requesterId,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) Integer maxDaysInactive,
            @RequestParam(defaultValue = "totalBalance") String orderBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                queryHandler.listAll(requesterId, minBalance, maxDaysInactive, orderBy, page, size));
    }
}
