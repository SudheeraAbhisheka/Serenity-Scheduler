package org.example.server1.controller;

import com.example.AlgorithmRequestObj;
import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.component.RabbitMQ_consumer;
import org.example.server1.service.SchedulingAlgorithms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;

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
    public void setAlgorithm(@RequestBody String algorithmRequest) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        AlgorithmRequestObj algorithmRequestObj = objectMapper.readValue(algorithmRequest, AlgorithmRequestObj.class);

        schedulingAlgorithms.setSchedulingAlgorithm(algorithmRequestObj);
        rabbitMQ_consumer.setSchedulingAlgorithm(algorithmRequestObj.getAlgorithm());
        kafka_consumer.setSchedulingAlgorithm(algorithmRequestObj.getAlgorithm());
    }

    @PostMapping("/set-priority-scheduling")
    public void setPriorityBased(@RequestBody LinkedHashMap<Integer, Double> thresholdTime) {
        schedulingAlgorithms.priorityBasedScheduling(thresholdTime);
        kafka_consumer.setSchedulingAlgorithm("age-based-priority-scheduling");

    }

    @PostMapping("/set-new-servers")
    public void addServers(@RequestBody LinkedHashMap<String, Double> newServers) {
        System.out.println("newServers: " + newServers);
        schedulingAlgorithms.addNewServersCATFModel(newServers);
    }
}
