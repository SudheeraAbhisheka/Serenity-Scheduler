//package org.example.user.component;
//
//
//
//import org.example.user.MessageTemplate;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.http.*;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Scanner;
//
//@Component
//class MessageSender implements CommandLineRunner {
//
//    private final RestTemplate restTemplate;
//
//    public MessageSender(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("Enter the message to send to Kafka:");
//        String userMessage = scanner.nextLine();
//
//        sendMessage(userMessage);
//    }
//
//    private void sendMessage(MessageTemplate messageTemplate) {
//        String url = "http://localhost:8080/send-message";
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        // Create an HttpEntity with the Template object and JSON headers
//        HttpEntity<MessageTemplate> request = new HttpEntity<>(messageTemplate, headers);
//
//        try {
//            // Send the request and capture the response
//            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
//            if (response.getStatusCode().is2xxSuccessful()) {
//                System.out.println("Message sent successfully: " + response.getBody());
//            } else {
//                System.out.println("Failed to send message. Status: " + response.getStatusCode());
//            }
//        } catch (Exception e) {
//            System.out.println("Exception occurred while sending message: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//}
//
