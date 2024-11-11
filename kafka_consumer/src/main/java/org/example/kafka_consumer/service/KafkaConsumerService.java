package org.example.kafka_consumer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.kafka_consumer.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicInteger;
import com.example.KeyValueObject;

@Service
public class KafkaConsumerService {

    AtomicInteger messageCount = new AtomicInteger(0);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @KafkaListener(topics = "topic_1-10", groupId = "my-group")
    public void listen_1to10(String message) {
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        try {
//            KeyValueObject keyValueObject = objectMapper.readValue(message, KeyValueObject.class);
//            System.out.println("Received message #" + messageCount + " from Kafka: " + keyValueObject);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
//
//        messageCount.incrementAndGet();

        String routingKey = "consumer.one";
        rabbitTemplate.convertAndSend(RabbitMQConfig.DIRECT_EXCHANGE, routingKey, message);



    }

    @KafkaListener(topics = "topic_11-21", groupId = "my-group")
    public void listen_11to21(String message) {
        String routingKey = "consumer.two";
        rabbitTemplate.convertAndSend(RabbitMQConfig.DIRECT_EXCHANGE, routingKey, message);

//        ObjectMapper objectMapper = new ObjectMapper();
//
//        try {
//            KeyValueObject keyValueObject = objectMapper.readValue(message, KeyValueObject.class);
//            System.out.println("Received message #" + messageCount + " from Kafka: " + keyValueObject);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
//
//        messageCount.incrementAndGet();
    }
}