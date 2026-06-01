package com.cqrs.domain.aggregate;

import com.cqrs.domain.command.CreateCustomerCommand;
import com.cqrs.domain.exception.DomainException;
import com.cqrs.entity.CustomerEntity;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CustomerAggregate {

    private UUID id;
    private String name;
    private String address;
    private int age;
    private UUID accountManagerId;

    public static CustomerAggregate create(CreateCustomerCommand cmd) {
        validateAge(cmd.age());
        CustomerAggregate agg = new CustomerAggregate();
        agg.id = UUID.randomUUID();
        agg.name = cmd.name();
        agg.address = cmd.address();
        agg.age = cmd.age();
        agg.accountManagerId = cmd.accountManagerId();
        return agg;
    }

    public static CustomerAggregate from(CustomerEntity entity) {
        CustomerAggregate agg = new CustomerAggregate();
        agg.id = entity.getId();
        agg.name = entity.getName();
        agg.address = entity.getAddress();
        agg.age = entity.getAge();
        agg.accountManagerId = entity.getAccountManagerId();
        return agg;
    }

    public void updateData(String name, String address, Integer age) {
        if (name != null) this.name = name;
        if (address != null) this.address = address;
        if (age != null) {
            validateAge(age);
            this.age = age;
        }
    }

    public void assignAccountManager(UUID accountManagerId) {
        this.accountManagerId = accountManagerId;
    }

    private static void validateAge(int age) {
        if (age < 18) {
            throw new DomainException("Customer must be at least 18 years old");
        }
    }
}
