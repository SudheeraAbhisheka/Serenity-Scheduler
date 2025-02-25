package org.example.kafka_server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.kafka_server.config.RabbitMQConfig;
import org.example.kafka_server.service.KafkaProducerService;
import org.example.kafka_server.service.MessageService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.example.TaskObject;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/kafka-server/set-broker")
    public void setMessageBroker(@RequestBody String messageBroker) {
        messageService.setMessageBroker(messageBroker);
    }

    @PostMapping("/kafka-server/set-message")
    public void setMessage(@RequestBody Map<String, Integer> map) {
        messageService.runTimedHelloWorld(map);
    }

    @PostMapping("/kafka-server/set-message-scheduled")
    public void setMessageScheduledRate(@RequestBody Map<String, Integer> mapScheduled) {
        messageService.runTimedHelloWorldScheduled(mapScheduled);
    }
}