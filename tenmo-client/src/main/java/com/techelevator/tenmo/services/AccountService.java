package com.techelevator.tenmo.services;


import com.techelevator.tenmo.model.User;
import com.techelevator.util.BasicLogger;
import org.springframework.http.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;


public class AccountService {

    private final String baseUrl;
    private final RestTemplate restTemplate = new RestTemplate();


    public AccountService(String url) {
        this.baseUrl = url + "account/";
    }

    public User[] getUsers(String token) {
        ResponseEntity<User[]> response =
                restTemplate.exchange(baseUrl, HttpMethod.GET, makeAuthToken(token), User[].class);
        return response.getBody();
    }

    public BigDecimal balance(String token, long id) {
        ResponseEntity<BigDecimal> response =
                restTemplate.exchange(baseUrl + id + "/balance", HttpMethod.GET, makeAuthToken(token), BigDecimal.class);
        return response.getBody();
    }

    public boolean send(String token, long fromId, int toId, BigDecimal amount) {
        String json = "{\"balance\": " +amount+ ",\"user_id\": " + fromId + "}";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        boolean success = false;
        try {
            restTemplate.exchange(baseUrl + "transferTo/" + toId, HttpMethod.PUT, entity, Void.class);
            success = true;
        } catch (RestClientResponseException | ResourceAccessException e) {
            BasicLogger.log(e.getMessage());
        }
        return success;
    }

    public HttpEntity<Void> makeAuthToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}

