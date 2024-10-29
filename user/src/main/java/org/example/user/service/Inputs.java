package org.example.user.service;

import org.example.user.MessageTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Scanner;

@Service
public class Inputs {
    private final RestTemplate restTemplate;

    public Inputs(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
        sendMessage(takingInputs());
    }

    public MessageTemplate takingInputs(){
        Scanner scanner = new Scanner(System.in);

        System.out.println("Type 1 & press Enter to upload");
        scanner.nextInt();

        MessageTemplate input1 = new MessageTemplate("4k video", 8, new int[]{5, 3, 2});
        MessageTemplate input2 = new MessageTemplate("1080p video", 4, new int[]{2, 4, 1});

        System.out.println("Done");

        return input1;

    }

    private void sendMessage(MessageTemplate messageTemplate) {
        String url = "http://localhost:8080/send-message";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create an HttpEntity with the Template object and JSON headers
        HttpEntity<MessageTemplate> request = new HttpEntity<>(messageTemplate, headers);

        try {
            // Send the request and capture the response
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Message sent successfully: " + response.getBody());
            } else {
                System.out.println("Failed to send message. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
