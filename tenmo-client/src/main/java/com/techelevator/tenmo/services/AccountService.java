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
                restTemplate.exchange(baseUrl + "accounts", HttpMethod.GET, makeAuthToken(token), User[].class);
        return response.getBody();
    }

    public BigDecimal balance(String token, long id) {
        ResponseEntity<BigDecimal> response =
                restTemplate.exchange(baseUrl + id + "/balance", HttpMethod.GET, makeAuthToken(token), BigDecimal.class);
        return response.getBody();
    }



    public HttpEntity<Void> makeAuthToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}

