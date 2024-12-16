package org.example.servers_terminal.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ServerService {
    private final RestTemplate restTemplate;
    private final AtomicInteger sendMessageCount = new AtomicInteger(0);
    private String url;
    private ScheduledExecutorService scheduler;

    public ServerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean setServers(int queueCapacity, LinkedHashMap<String, Double> servers){
        String url = "http://localhost:8084/api/set-servers?queueCapacity=" + queueCapacity;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LinkedHashMap<String, Double>> request = new HttpEntity<>(servers, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // Just return false; the FxController will log the message returned by this method if needed
            return false;
        }
    }

    public boolean setNewServers(LinkedHashMap<String, Double> newServers){
        String url = "http://localhost:8084/api/set-new-servers";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LinkedHashMap<String, Double>> request = new HttpEntity<>(newServers, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
