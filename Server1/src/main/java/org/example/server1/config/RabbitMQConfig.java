package org.example.server1.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DIRECT_EXCHANGE = "direct.exchange";
    public static final String CONSUMER_ONE_QUEUE = "consumer.one.queue";
    public static final String CONSUMER_TWO_QUEUE = "consumer.two.queue";

    @Bean
    public Queue consumerOneQueue() {
        return new Queue(CONSUMER_ONE_QUEUE, false);
    }

    @Bean
    public Queue consumerTwoQueue() {
        return new Queue(CONSUMER_TWO_QUEUE, false);
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(DIRECT_EXCHANGE);
    }

    @Bean
    public Binding bindingConsumerOne(Queue consumerOneQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(consumerOneQueue).to(directExchange).with("consumer.one");
    }

    @Bean
    public Binding bindingConsumerTwo(Queue consumerTwoQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(consumerTwoQueue).to(directExchange).with("consumer.two");
    }
}
