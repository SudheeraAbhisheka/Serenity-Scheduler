package org.example.kafka_server.controller;

import org.example.kafka_server.service.KafkaProducerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final KafkaProducerService kafkaProducerService;

    public MessageController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/send-message")
    public String sendMessage(@RequestBody MessageTemplate messageTemplate) {
        kafkaProducerService.sendMessage("my-topic", messageTemplate);
        return "Message sent!";
    }
}