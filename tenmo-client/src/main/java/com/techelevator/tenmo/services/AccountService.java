package com.techelevator.tenmo.services;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;

public class AccountService {

    private final String baseUrl;
    private final RestTemplate restTemplate = new RestTemplate();


    public AccountService(String url) {this.baseUrl = url + "account/";}

    public BigDecimal balance(String token, long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<BigDecimal> response = restTemplate.exchange(baseUrl + id + "/balance", HttpMethod.GET, entity,BigDecimal.class);
        return response.getBody();
    }
}
