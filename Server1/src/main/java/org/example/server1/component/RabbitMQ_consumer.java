package org.example.server1.component;

import lombok.Getter;
import lombok.Setter;
import org.example.server1.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class RabbitMQ_consumer {
    @Getter
    private static final BlockingQueue<String> blockingQueueCompleteF = new LinkedBlockingQueue<>(1);
    @Getter
    private static final BlockingQueue<String> blockingQueuePriorityS = new LinkedBlockingQueue<>();
    @Setter
    private String schedulingAlgorithm;

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_ONE_QUEUE)
    public void receiveMessageFromConsumerOneQueue(String message) {
        switch(schedulingAlgorithm){
            case "complete-and-then-fetch": {
                try {
                    blockingQueueCompleteF.put(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Reset interrupt status
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

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_TWO_QUEUE)
    public void receiveMessageFromConsumerTwoQueue(String message) {


    }
}


