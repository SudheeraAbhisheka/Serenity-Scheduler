package org.example.server1.component;

import org.example.server1.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumerOne {

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_ONE_QUEUE)
    public void receiveMessageFromConsumerOneQueue(String message) {
        System.out.println("Consumer 1 received message: " + message);
    }

    @RabbitListener(queues = RabbitMQConfig.CONSUMER_TWO_QUEUE)
    public void receiveMessageFromConsumerTwoQueue(String message) {
        System.out.println("Consumer 2 received message: " + message);
    }
}


