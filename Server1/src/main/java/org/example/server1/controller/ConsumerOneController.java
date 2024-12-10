package org.example.server1.controller;

import org.example.server1.component.Kafka_consumer;
import org.example.server1.component.RabbitMQ_consumer;
import org.example.server1.service.SchedulingAlgorithms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consumer-one")
public class ConsumerOneController {

    private final SchedulingAlgorithms schedulingAlgorithms;
    private final RabbitMQ_consumer rabbitMQ_consumer;
    private final Kafka_consumer kafka_consumer;

    @Autowired
    public ConsumerOneController(SchedulingAlgorithms schedulingAlgorithms, RabbitMQ_consumer rabbitMQ_consumer, Kafka_consumer kafka_consumer) {
        this.schedulingAlgorithms = schedulingAlgorithms;
        this.rabbitMQ_consumer = rabbitMQ_consumer;
        this.kafka_consumer = kafka_consumer;
    }

    @PostMapping("/set-algorithm")
    public void setAlgorithm(@RequestBody String schedulingAlgorithm) {
        schedulingAlgorithms.setSchedulingAlgorithm(schedulingAlgorithm);
        rabbitMQ_consumer.setSchedulingAlgorithm(schedulingAlgorithm);
        kafka_consumer.setSchedulingAlgorithm(schedulingAlgorithm);
    }
}
