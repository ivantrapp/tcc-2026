package com.cqrs.controller;

import com.cqrs.domain.command.CreateWalletCommand;
import com.cqrs.domain.command.RegisterTransactionCommand;
import com.cqrs.handler.CreateWalletHandler;
import com.cqrs.handler.RegisterTransactionHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final CreateWalletHandler createHandler;
    private final RegisterTransactionHandler transactionHandler;

    @PostMapping
    public ResponseEntity<Map<String, UUID>> create(@Valid @RequestBody CreateWalletCommand command) {
        UUID id = createHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("walletId", id));
    }

    @PostMapping("/{id}/transactions")
    public ResponseEntity<Void> registerTransaction(@PathVariable UUID id,
                                                    @Valid @RequestBody RegisterTransactionCommand body) {
        transactionHandler.handle(new RegisterTransactionCommand(id, body.value(), body.type(),
                body.exchangeConfirmationId(), body.correlationId(), body.causedBy()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
