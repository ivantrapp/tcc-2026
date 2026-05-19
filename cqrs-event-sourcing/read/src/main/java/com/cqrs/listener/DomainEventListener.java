package com.cqrs.listener;

import com.cqrs.config.RabbitMQConfig;
import com.cqrs.event.*;
import com.cqrs.projector.AccountManagerProjector;
import com.cqrs.projector.CustomerProjector;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMQConfig.QUEUE)
@RequiredArgsConstructor
public class DomainEventListener {

    private final CustomerProjector customerProjector;
    private final AccountManagerProjector accountManagerProjector;

    @RabbitHandler
    public void on(CustomerCreatedMessage event) {
        customerProjector.on(event);
    }

    @RabbitHandler
    public void on(CustomerDataUpdatedMessage event) {
        customerProjector.on(event);
    }

    @RabbitHandler
    public void on(AccountManagerAssignedMessage event) {
        customerProjector.on(event);
    }

    @RabbitHandler
    public void on(WalletCreatedMessage event) {
        customerProjector.on(event);
    }

    @RabbitHandler
    public void on(TransactionRegisteredMessage event) {
        customerProjector.on(event);
    }

    @RabbitHandler
    public void on(AccountManagerCreatedMessage event) {
        accountManagerProjector.on(event);
    }

    @RabbitHandler
    public void on(AdminProfileGrantedMessage event) {
        accountManagerProjector.on(event);
    }

    @RabbitHandler
    public void on(BranchAssignedMessage event) {
        accountManagerProjector.on(event);
    }

    @RabbitHandler(isDefault = true)
    public void unknown(Object message) {}
}
