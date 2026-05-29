package com.cqrs.controller;

import com.cqrs.domain.command.AssignAccountManagerCommand;
import com.cqrs.domain.command.CreateCustomerCommand;
import com.cqrs.domain.command.UpdateCustomerDataCommand;
import com.cqrs.eventstore.EventStore;
import com.cqrs.handler.AssignAccountManagerHandler;
import com.cqrs.handler.CreateCustomerHandler;
import com.cqrs.handler.UpdateCustomerDataHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CreateCustomerHandler createHandler;
    private final UpdateCustomerDataHandler updateHandler;
    private final AssignAccountManagerHandler assignManagerHandler;
    private final EventStore eventStore;

    @PostMapping
    public ResponseEntity<Map<String, UUID>> create(@Valid @RequestBody CreateCustomerCommand command) {
        UUID id = createHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("customerId", id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateCustomerDataCommand body) {
        updateHandler.handle(new UpdateCustomerDataCommand(id, body.name(), body.address(), body.age(),
                body.correlationId(), body.causedBy()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/account-manager")
    public ResponseEntity<Void> assignManager(@PathVariable UUID id,
                                              @Valid @RequestBody AssignAccountManagerCommand body) {
        assignManagerHandler.handle(new AssignAccountManagerCommand(id, body.accountManagerId(),
                body.correlationId(), body.causedBy()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Void> exists(@PathVariable UUID id) {
        return eventStore.streamExists("Customer-" + id)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
