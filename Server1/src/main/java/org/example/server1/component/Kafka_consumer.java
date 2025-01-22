package org.example.server1.component;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.example.server1.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class Kafka_consumer {
    @Getter
    private final BlockingQueue<KeyValueObject> blockingQueueCompleteF = new LinkedBlockingQueue<>(1);
    @Getter
    private final BlockingQueue<KeyValueObject> blockingQueuePriorityS = new LinkedBlockingQueue<>();
    @Setter
    private String schedulingAlgorithm;
    @Getter
    private final ConcurrentLinkedQueue<KeyValueObject> wlbQueue = new ConcurrentLinkedQueue<>();
    @Setter
    private boolean crashedTasks = false;
    @Getter
    private final Object lock = new Object();
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "topic_1-10", groupId = "my-group")
    public void listen_1to10(String message) throws InterruptedException, JsonProcessingException {
        selectingAlgorithm(objectMapper.readValue(message, KeyValueObject.class));
    }

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_ONE_QUEUE)
    public void receiveMessageFromConsumerOneQueue(String message) throws InterruptedException, JsonProcessingException {
        selectingAlgorithm(objectMapper.readValue(message, KeyValueObject.class));
    }

    private void selectingAlgorithm(KeyValueObject task) throws InterruptedException {
        if(crashedTasks){
            synchronized(lock){
                System.out.println("blocking......");
                lock.wait();
            }
        }

        switch(schedulingAlgorithm){
            case "complete-and-then-fetch": {
                blockingQueueCompleteF.put(task);

                break;
            }

            case "age-based-priority-scheduling": {
                blockingQueuePriorityS.add(task);
                break;
            }

            case "weight-load-balancing" :
                wlbQueue.add(task);
                break;

            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + schedulingAlgorithm);

        }
    }
}