package com.techelevator.tenmo.services;

import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferHistory;
import com.techelevator.tenmo.model.User;
import com.techelevator.util.BasicLogger;
import org.springframework.http.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;


public class TransferService {

    private final String baseUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public TransferService (String url) {this.baseUrl = url + "account/";}

    public Transfer send(String token, User user,long toId, BigDecimal amount) {
        Transfer transfer = new Transfer();
        transfer.setTransfer_id(user.getId());
        transfer.setBalance(amount);
        transfer.setUsername(user.getUsername());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Transfer> entity = new HttpEntity<>(transfer, headers);
        try {
            ResponseEntity<Transfer> response = restTemplate.exchange(baseUrl + "transfer/" + toId, HttpMethod.PUT, entity, Transfer.class);
            transfer = response.getBody();
        } catch (RestClientResponseException | ResourceAccessException e) {
            BasicLogger.log(e.getMessage());
        }
        return transfer;
    }

    public TransferHistory[] history(String token, Long id) {
        TransferHistory[] transfer = new TransferHistory[0];
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<TransferHistory[]> response = restTemplate.exchange(baseUrl + "history/" + id, HttpMethod.GET, entity, TransferHistory[].class);
            transfer = response.getBody();
        } catch (RestClientResponseException | ResourceAccessException e) {
            BasicLogger.log(e.getMessage());
        }
        return transfer;
    }
}
