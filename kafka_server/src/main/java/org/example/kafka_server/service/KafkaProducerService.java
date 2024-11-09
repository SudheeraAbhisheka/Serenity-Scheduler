package org.example.kafka_server.service;

import com.example.KeyValueObject;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, KeyValueObject> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, KeyValueObject> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, KeyValueObject keyValueObject) {
        kafkaTemplate.send(topic, keyValueObject);
        System.out.println("Message sent to Kafka topic: " + topic);
    }
}

