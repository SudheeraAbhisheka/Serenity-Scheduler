package org.example.kafka_server.service;

import com.example.TaskObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.example.kafka_server.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MessageService {
    private final KafkaProducerService kafkaProducerService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicInteger sendMessageCount = new AtomicInteger(0);
    private ScheduledExecutorService scheduler;
    @Setter
    private String messageBroker;
    private final RabbitTemplate rabbitTemplate;

    public MessageService(KafkaProducerService kafkaProducerService, RabbitTemplate rabbitTemplate) {
        this.kafkaProducerService = kafkaProducerService;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(TaskObject task) {
        switch(messageBroker){
            case "kafka" : {
                kafkaProducerService.sendMessage("topic_1-10", task);
                break;
            }
            case "rabbitmq" : {
                String routingKey = "consumer.one";

                ObjectMapper objectMapper = new ObjectMapper();
                String message = null;
                try {
                    message = objectMapper.writeValueAsString(task);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                rabbitTemplate.convertAndSend(RabbitMQConfig.DIRECT_EXCHANGE, routingKey, message);

                break;
            }
            default:{
                kafkaProducerService.sendMessage("topic_1-10", task);
                break;
            }
        }
    }

    public void runTimedHelloWorld(Map<String, Integer> map) {
        int noOfThreads = map.get("noOfThreads");
        int noOfTasks = map.get("noOfTasks");
        int minWeight = map.get("minWeight");
        int maxWeight = map.get("maxWeight");
        int minPriority = map.get("minPriority");
        int maxPriority = map.get("maxPriority");

        Random random = new Random(12345);
        sendMessageCount.set(0);

        ArrayList<Integer> noOfTasksList = new ArrayList<>();
        int tasksPerThread = noOfTasks / noOfThreads;
        int remainder = noOfTasks % noOfThreads;

        for (int i = 0; i < noOfThreads; i++) {
            if (i == noOfThreads - 1) {
                noOfTasksList.add(tasksPerThread + remainder);
            } else {
                noOfTasksList.add(tasksPerThread);
            }
        }

        for (int tasksPerThreadCorrect : noOfTasksList) {
            executor.submit(() -> {
                for (int i = 0; i < tasksPerThreadCorrect; i++) {
                    TaskObject task = new TaskObject(
                            UUID.randomUUID().toString(),
                            1 + random.nextInt(10),
                            minWeight + random.nextInt(maxWeight - minWeight + 1),
                            false,
                            minPriority + random.nextInt(maxPriority - minPriority + 1),
                            System.currentTimeMillis()
                    );

                    sendMessage(task);

                    int currentCount = sendMessageCount.incrementAndGet();
                    if (currentCount == noOfTasks) {
                        System.out.println("Done");
                    }
                }

            });
        }


    }

    public void runTimedHelloWorldScheduled(Map<String, Integer> map) {

        int scheduleRate = map.get("scheduleRate");
        int executionDuration = map.get("executionDuration");
        int minWeight = map.get("minWeight");
        int maxWeight = map.get("maxWeight");
        int minPriority = map.get("minPriority");
        int maxPriority = map.get("maxPriority");

        Random random = new Random(12345);

        int noOfTasks = (executionDuration * 1000) / scheduleRate;
        sendMessageCount.set(0);

        executor.submit(() -> {
            for(int i = 0; i < noOfTasks; i++){
                TaskObject task = new TaskObject(
                        UUID.randomUUID().toString(),
                        1 + random.nextInt(10),
                        minWeight + random.nextInt(maxWeight - minWeight + 1),
                        false,
                        minPriority + random.nextInt(maxPriority - minPriority + 1),
                        System.currentTimeMillis()
                );

                sendMessage(task);

                if (sendMessageCount.incrementAndGet() != noOfTasks) {
                    try {
                        Thread.sleep(scheduleRate);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        });
    }

}
