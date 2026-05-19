package com.cqrs.handler;

import com.cqrs.domain.aggregate.CustomerAggregate;
import com.cqrs.domain.command.UpdateCustomerDataCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateCustomerDataHandler {

    private final CustomerRepository customerRepository;

    public void handle(UpdateCustomerDataCommand cmd) {
        customerRepository.findById(cmd.customerId())
                .map(entity -> {
                    CustomerAggregate aggregate = CustomerAggregate.from(entity);
                    aggregate.updateData(cmd.name(), cmd.address(), cmd.age());
                    entity.setName(aggregate.getName());
                    entity.setAddress(aggregate.getAddress());
                    entity.setAge(aggregate.getAge());
                    return entity;
                })
                .orElseThrow(() -> new DomainException("Customer not found: " + cmd.customerId()));
    }
}
