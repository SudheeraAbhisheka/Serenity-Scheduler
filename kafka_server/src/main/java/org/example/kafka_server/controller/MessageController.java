package org.example.kafka_server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.kafka_server.config.RabbitMQConfig;
import org.example.kafka_server.service.KafkaProducerService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.KeyValueObject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
public class MessageController {

    private final KafkaProducerService kafkaProducerService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public MessageController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @PostMapping("/kafka-server/topic_1-10")
    public String kafka_sendMessage1(@RequestBody KeyValueObject keyValueObject) throws InterruptedException {
        kafkaProducerService.sendMessage("topic_1-10", keyValueObject);

//        System.out.println(Thread.currentThread().getId() + " started");
//        Thread.sleep(500);
//        System.out.println(Thread.currentThread().getId());
//        System.out.println();

        return "Message sent!";
    }
//
//    @PostMapping("/send-message/topic_11-20")
//    public String sendMessage11_21(@RequestBody KeyValueObject keyValueObject) {
//        kafkaProducerService.sendMessage("topic_11-20", keyValueObject);
//        return "Message sent!";
//    }

    @PostMapping("/rebbitmq-server/topic_1-10")
    public String rabbitmq_sendMessage1(@RequestBody KeyValueObject keyValueObject) throws JsonProcessingException {
        String routingKey = "consumer.one";

        ObjectMapper objectMapper = new ObjectMapper();
        String message = objectMapper.writeValueAsString(keyValueObject);

        rabbitTemplate.convertAndSend(RabbitMQConfig.DIRECT_EXCHANGE, routingKey, message);
        return "Message sent!";
    }

//    @PostMapping("/send-message/topic_11-20")
//    public String sendMessage11_21(@RequestBody KeyValueObject keyValueObject) {
//        kafkaProducerService.sendMessage("topic_11-20", keyValueObject);
//        return "Message sent!";
//    }
}