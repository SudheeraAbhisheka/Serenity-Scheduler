package org.example.server1.component;

import lombok.Getter;
import lombok.Setter;
import org.example.server1.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class RabbitMQ_consumer {
    @Getter
    private static final BlockingQueue<String> blockingQueueCompleteF = new LinkedBlockingQueue<>(1);
    @Getter
    private static final BlockingQueue<String> blockingQueuePriorityS = new LinkedBlockingQueue<>();
    @Setter
    private String schedulingAlgorithm;
    @Getter
    private static final ConcurrentLinkedQueue<String> wlbQueue = new ConcurrentLinkedQueue<>();

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_ONE_QUEUE)
    public void receiveMessageFromConsumerOneQueue(String message) throws InterruptedException {
        switch(schedulingAlgorithm){
            case "complete-and-then-fetch": {
                blockingQueueCompleteF.put(message);
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


