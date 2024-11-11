package org.example.kafka_server.controller;

import org.example.kafka_server.service.KafkaProducerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.KeyValueObject;

@RestController
public class MessageController {

    private final KafkaProducerService kafkaProducerService;

    public MessageController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/send-message/topic_1-10")
    public String sendMessage1_10(@RequestBody KeyValueObject keyValueObject) {
        kafkaProducerService.sendMessage("topic_1-10", keyValueObject);
        return "Message sent!";
    }

    @PostMapping("/send-message/topic_11-21")
    public String sendMessage11_21(@RequestBody KeyValueObject keyValueObject) {
        kafkaProducerService.sendMessage("topic_11-21", keyValueObject);
        return "Message sent!";
    }
}