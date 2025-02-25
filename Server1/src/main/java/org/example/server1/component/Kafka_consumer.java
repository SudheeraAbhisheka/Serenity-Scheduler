package org.example.server1.component;

import com.example.TaskObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.example.server1.config.RabbitMQConfig;
import org.example.server1.service.PriorityBasedScheduling;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class Kafka_consumer {
    @Getter
    private final BlockingQueue<TaskObject> blockingQueueCompleteF = new LinkedBlockingQueue<>(1);
    @Getter
    private final BlockingQueue<TaskObject> blockingQueuePriorityS = new LinkedBlockingQueue<>();
    @Getter
    private final BlockingQueue<TaskObject> wlbQueue = new LinkedBlockingQueue<>();
    private String schedulingAlgorithm = "";
    @Setter
    private boolean crashedTasks = true;
    @Getter
    private final Object lock = new Object();
    ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "topic_1-10", groupId = "my-group")
    public void listen_1to10(String message) throws InterruptedException, JsonProcessingException {
        selectingAlgorithm(objectMapper.readValue(message, TaskObject.class));
    }

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_ONE_QUEUE)
    public void receiveMessageFromConsumerOneQueue(String message) throws InterruptedException, JsonProcessingException {
        selectingAlgorithm(objectMapper.readValue(message, TaskObject.class));
    }

    private void selectingAlgorithm(TaskObject task) throws InterruptedException {
        if(crashedTasks){
            synchronized(lock){
                System.out.println("blocking......");
                lock.wait();
            }
        }

        switch(schedulingAlgorithm){
            case "complete-and-then-fetch":
                blockingQueueCompleteF.put(task);
                break;

            case "weight-load-balancing" :
                wlbQueue.add(task);
                break;

            case "priority-complete-fetch", "priority-load-balancing":
                blockingQueuePriorityS.add(task);
                break;

            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + schedulingAlgorithm);

        }
    }

    public void setSchedulingAlgorithm(String schedulingAlgorithm){
        crashedTasks = false;
        synchronized(lock){
            lock.notify();
        }
        this.schedulingAlgorithm = schedulingAlgorithm;
    }
}