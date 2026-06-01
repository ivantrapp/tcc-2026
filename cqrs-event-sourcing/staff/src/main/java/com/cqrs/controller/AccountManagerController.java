package com.cqrs.controller;

import com.cqrs.domain.command.AssignBranchCommand;
import com.cqrs.domain.command.CreateAccountManagerCommand;
import com.cqrs.domain.command.GrantAdminProfileCommand;
import com.cqrs.handler.AssignBranchHandler;
import com.cqrs.handler.CreateAccountManagerHandler;
import com.cqrs.handler.GrantAdminProfileHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/account-managers")
@RequiredArgsConstructor
public class AccountManagerController {

    private final CreateAccountManagerHandler createHandler;
    private final GrantAdminProfileHandler grantAdminHandler;
    private final AssignBranchHandler assignBranchHandler;

    @PostMapping
    public ResponseEntity<Map<String, UUID>> create(@Valid @RequestBody CreateAccountManagerCommand command) {
        UUID id = createHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("accountManagerId", id));
    }

    @PutMapping("/{id}/admin")
    public ResponseEntity<Void> grantAdmin(@PathVariable UUID id,
                                           @RequestBody(required = false) GrantAdminProfileCommand body) {
        UUID correlationId = body != null ? body.correlationId() : null;
        String causedBy = body != null ? body.causedBy() : null;
        grantAdminHandler.handle(new GrantAdminProfileCommand(id, correlationId, causedBy));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/branch")
    public ResponseEntity<Void> assignBranch(@PathVariable UUID id,
                                             @Valid @RequestBody AssignBranchCommand body) {
        assignBranchHandler.handle(new AssignBranchCommand(id, body.branchId(), body.correlationId(), body.causedBy()));
        return ResponseEntity.noContent().build();
    }

}
