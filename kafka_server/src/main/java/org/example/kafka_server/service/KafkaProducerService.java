package org.example.kafka_server.service;

import com.example.TaskObject;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, TaskObject> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, TaskObject> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, TaskObject task) {
        kafkaTemplate.send(topic, task);
    }
}

