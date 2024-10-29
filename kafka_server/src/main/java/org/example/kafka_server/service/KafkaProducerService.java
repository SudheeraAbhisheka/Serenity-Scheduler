package org.example.kafka_server.service;

import org.example.kafka_server.controller.MessageTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, MessageTemplate> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, MessageTemplate> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, MessageTemplate messageTemplate) {
        kafkaTemplate.send(topic, messageTemplate);
        System.out.println("Message sent to Kafka topic: " + topic);
    }
}

