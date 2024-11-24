package org.example.server1.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server1.component.ConsumerOne;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ServerSimulator {

    private final RestTemplate restTemplate;

    public ServerSimulator() {
        this.restTemplate = new RestTemplate();

        new Thread(() -> {
            while (true) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject;
                String message;

                try {
                    message = ConsumerOne.getMessageBlockingQueue1().take();
                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                    sendToServer1(keyValueObject);

                } catch (InterruptedException | JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        new Thread(() -> {
            while (true) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject;
                String message;

                try {
                    message = ConsumerOne.getMessageBlockingQueue1().take();
                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                    sendToServer2(keyValueObject);

                } catch (InterruptedException | JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void sendToServer1(KeyValueObject keyValueObject) {
        try {
            String url = "http://servers:8084/api/server1";
            ResponseEntity<String> response = restTemplate.postForEntity(url, keyValueObject, String.class);

        } catch (Exception e) {
            System.err.printf("Error sending to Server 1: %s\n", e.getMessage());
        }
    }

    private void sendToServer2(KeyValueObject keyValueObject) {
        try {
            String url = "http://servers:8084/api/server2";
            ResponseEntity<String> response = restTemplate.postForEntity(url, keyValueObject, String.class);

        } catch (Exception e) {
            System.err.printf("Error sending to Server 2: %s\n", e.getMessage());
        }
    }
}
