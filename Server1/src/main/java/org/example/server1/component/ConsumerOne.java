package org.example.server1.component;

import lombok.Getter;
import lombok.Setter;
import org.example.server1.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ConsumerOne {
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
            }
            case "priority-based-scheduling": {
                blockingQueuePriorityS.offer(message);
            }

        }



    }

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_TWO_QUEUE)
    public void receiveMessageFromConsumerTwoQueue(String message) {


    }
}


