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

    public boolean setServers(LinkedHashMap<String, Double> servers){
        String url = "http://localhost:8084/api/set-servers";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<LinkedHashMap<String, Double>> request = new HttpEntity<>(servers, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Servers set successfully. " + servers.size());
                setSuccess = true;
            } else {
                System.out.println("Failed to set algorithm. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while setting algorithm: " + e.getMessage());
            e.printStackTrace();
        }

        return setSuccess;
    }

    public boolean setNewServers(LinkedHashMap<String, Double> newServers){
        String url = "http://localhost:8084/api/set-new-servers";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<LinkedHashMap<String, Double>> request = new HttpEntity<>(newServers, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("New servers set successfully.");
                setSuccess = true;
            } else {
                System.out.println("Failed to set algorithm. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while setting algorithm: " + e.getMessage());
            e.printStackTrace();
        }

        return setSuccess;
    }

}
