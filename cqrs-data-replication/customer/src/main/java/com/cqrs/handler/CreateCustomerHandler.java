package com.cqrs.handler;

import com.cqrs.client.StaffClient;
import com.cqrs.domain.aggregate.CustomerAggregate;
import com.cqrs.domain.command.CreateCustomerCommand;
import com.cqrs.entity.CustomerEntity;
import com.cqrs.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateCustomerHandler {

    private final CustomerRepository customerRepository;
    private final StaffClient staffClient;

    public UUID handle(CreateCustomerCommand cmd) {
        staffClient.validateAccountManagerExists(cmd.accountManagerId());

        CustomerAggregate aggregate = CustomerAggregate.create(cmd);

        CustomerEntity entity = new CustomerEntity();
        entity.setId(aggregate.getId());
        entity.setName(aggregate.getName());
        entity.setAddress(aggregate.getAddress());
        entity.setAge(aggregate.getAge());
        entity.setAccountManagerId(aggregate.getAccountManagerId());
        entity.setCreatedAt(Instant.now());
        customerRepository.save(entity);

        return entity.getId();
    }
}
