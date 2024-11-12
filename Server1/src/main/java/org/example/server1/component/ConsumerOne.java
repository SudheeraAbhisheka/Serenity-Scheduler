package org.example.server1.component;

import lombok.Getter;
import org.example.server1.config.RabbitMQConfig;
import org.example.server1.service.ServerSimulator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ConsumerOne {
    @Getter
    private static final BlockingQueue<String> messageBlockingQueue1 = new LinkedBlockingQueue<>(1);

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_ONE_QUEUE)
    public void receiveMessageFromConsumerOneQueue(String message) {
//        System.out.println("Consumer 1 received message: " + message);

        try {
            messageBlockingQueue1.put(message); // Using put to handle blocking behavior
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Reset interrupt status
            System.out.println("Thread was interrupted while adding a message to the queue.");
        }

    }

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_TWO_QUEUE)
    public void receiveMessageFromConsumerTwoQueue(String message) {


    }
}


