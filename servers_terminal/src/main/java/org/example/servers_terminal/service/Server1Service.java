package org.example.servers_terminal.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;

@Service
public class Server1Service {
    private final RestTemplate restTemplate;

    public Server1Service(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean setCompleteAndFetch() {
        String url = "http://localhost:8083/consumer-one/set-complete-and-fetch";
        HttpHeaders headers = new HttpHeaders();
        boolean setSuccess = false;

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Priority-based scheduling set successfully");
                setSuccess = true;
            } else {
                System.out.println("Failed to set Priority-based scheduling. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while setting PBS: " + e.getMessage());
            e.printStackTrace();
        }

        return setSuccess;
    }

    public boolean setPriorityScheduling(LinkedHashMap<Integer, Double> thresholdTime){
        String url = "http://localhost:8083/consumer-one/set-priority-scheduling";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<LinkedHashMap<Integer, Double>> request = new HttpEntity<>(thresholdTime, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Priority based scheduling set successfully");
                setSuccess = true;
            } else {
                System.out.println("Failed to set Priority based scheduling. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while setting pbs: " + e.getMessage());
            e.printStackTrace();
        }

        return setSuccess;
    }

    public boolean setWorkLoadBalancing(int fixedRate){
        String url = "http://localhost:8083/consumer-one/set-work-load-balancing";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<Integer> request = new HttpEntity<>(fixedRate, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Priority based scheduling set successfully");
                setSuccess = true;
            } else {
                System.out.println("Failed to set Priority based scheduling. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while setting pbs: " + e.getMessage());
            e.printStackTrace();
        }

        return setSuccess;
    }
}
