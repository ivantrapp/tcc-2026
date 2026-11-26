package com.cqrs.client;

import com.cqrs.domain.exception.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class StaffClient {

    private final RestClient restClient;

    public StaffClient(@Value("${services.read.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validateAccountManagerExists(UUID id) {
        try {
            restClient.get()
                    .uri("/account-managers/{id}", id)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new DomainException("Account manager not found: " + id);
            }
            throw e;
        }
    }
}
