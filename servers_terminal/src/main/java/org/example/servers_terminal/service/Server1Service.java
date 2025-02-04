package org.example.servers_terminal.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public boolean setLoadBalancing(){
        String url = "http://localhost:8083/consumer-one/set-load-balancing";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<Void> request = new HttpEntity<>(headers);

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

    public boolean setPriorityCompleteFetch(LinkedHashMap<Integer, Long> thresholdTime){
        String url = "http://localhost:8083/consumer-one/set-priority-complete-fetch";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<LinkedHashMap<Integer, Long>> request = new HttpEntity<>(thresholdTime, headers);

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

    public boolean setPriorityLoadBalancing(LinkedHashMap<Integer, Long> thresholdTime){
        String url = "http://localhost:8083/consumer-one/set-priority-load-balancing";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<LinkedHashMap<Integer, Long>> request = new HttpEntity<>(thresholdTime, headers);

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

    public boolean updateLoadBalancingWaitTime(Long waitTime){
        String url = "http://localhost:8083/consumer-one/update-wait-time";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<Long> request = new HttpEntity<>(waitTime, headers);

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

    /*public boolean updateLoadBalancingWaitTimes(LinkedHashMap<String, Long> waitTimes){
        LinkedHashMap<String, Long> newHashMap = new LinkedHashMap<>(Map.of(
                                "waitTime1", 500L,
                                "waitTime2", 10000L
                        ));

        String url = "http://localhost:8083/consumer-one/update-load-balancing-wait-times";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<LinkedHashMap<String, Long>> request = new HttpEntity<>(waitTimes, headers);

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
    }*/

}
