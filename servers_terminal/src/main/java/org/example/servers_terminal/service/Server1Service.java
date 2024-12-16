package org.example.servers_terminal.service;

import com.example.AlgorithmRequestObj;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class Server1Service {
    private final RestTemplate restTemplate;

    public Server1Service(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean setAlgorithm(String algorithm, String messageBroker){
        String url = "http://localhost:8083/consumer-one/set-algorithm";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        AlgorithmRequestObj requestBody = new AlgorithmRequestObj(algorithm, messageBroker);

        HttpEntity<AlgorithmRequestObj> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Algorithm set successfully - " + algorithm);
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
