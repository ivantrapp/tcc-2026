package com.cqrs.client;

import com.cqrs.domain.exception.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(@Value("${services.read.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validateCustomerExists(UUID id) {
        try {
            restClient.get()
                    .uri("/customers/{id}", id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new DomainException("Customer not found: " + id);
            }
            throw e;
        }
    }
}
