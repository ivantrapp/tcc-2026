package com.cqrs.controller;

import com.cqrs.domain.command.CreateBranchCommand;
import com.cqrs.handler.CreateBranchHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final CreateBranchHandler createHandler;

    @PostMapping
    public ResponseEntity<Map<String, UUID>> create(@Valid @RequestBody CreateBranchCommand command) {
        UUID id = createHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("branchId", id));
    }
}
