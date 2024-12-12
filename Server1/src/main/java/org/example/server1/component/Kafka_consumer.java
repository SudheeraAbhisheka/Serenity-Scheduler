package org.example.server1.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class Kafka_consumer {
    @Getter
    private static final BlockingQueue<String> blockingQueueCompleteF = new LinkedBlockingQueue<>(1);
    @Getter
    private static final BlockingQueue<String> blockingQueuePriorityS = new LinkedBlockingQueue<>();
    @Setter
    private String schedulingAlgorithm;

    @KafkaListener(topics = "topic_1-10", groupId = "my-group")
    public void listen_1to10(String message) {
        switch(schedulingAlgorithm){
            case "complete-and-then-fetch": {
                try {
                    blockingQueueCompleteF.put(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("blockingQueueCompleteF was interrupted while adding a message to the queue.");
                }

                break;
            }
            case "age-based-priority-scheduling": {
                blockingQueuePriorityS.offer(message);

                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + schedulingAlgorithm);

        }
    }

//    @KafkaListener(topics = "topic_11-20", groupId = "my-group")
//    public void listen_11to21(String message) {
//        String routingKey = "consumer.two";
//        rabbitTemplate.convertAndSend(RabbitMQConfig.DIRECT_EXCHANGE, routingKey, message);
//
//    }
}