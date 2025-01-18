package org.example.server1.component;

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
    private static final BlockingQueue<String> blockingQueueCompleteF = new LinkedBlockingQueue<>(1);
    @Getter
    private static final BlockingQueue<String> blockingQueuePriorityS = new LinkedBlockingQueue<>();
    @Setter
    private String schedulingAlgorithm;
    @Getter
    private static final ConcurrentLinkedQueue<String> wlbQueue = new ConcurrentLinkedQueue<>();
    @Setter
    private static boolean crashedTasks = false;
    @Getter
    private static final Object lock = new Object();

    @KafkaListener(topics = "topic_1-10", groupId = "my-group")
    public void listen_1to10(String message) throws InterruptedException {
        if(crashedTasks){
            synchronized(lock){
                System.out.println("Locking..");
                lock.wait();
                System.out.println("Unlocking..");
            }
        }

        switch(schedulingAlgorithm){
            case "complete-and-then-fetch": {
                if (false) {
                    blockingQueueCompleteF.put(message);
                }
                break;
            }

            case "age-based-priority-scheduling": {
                blockingQueuePriorityS.offer(message);
                break;
            }

            case "weight-load-balancing" :
                wlbQueue.add(message);
            break;

            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + schedulingAlgorithm);

        }
    }
}