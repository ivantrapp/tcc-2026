package com.cqrs.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class AccountManagerClient {

    private final RestClient restClient;

    public AccountManagerClient(@Value("${services.read.base-url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public boolean exists(UUID accountManagerId) {
        try {
            restClient.get()
                    .uri("/account-managers/{id}", accountManagerId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }
}
