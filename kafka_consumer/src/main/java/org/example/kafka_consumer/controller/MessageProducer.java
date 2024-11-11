//package org.example.kafka_consumer.controller;
//
//import org.example.kafka_consumer.config.RabbitMQConfig;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class MessageProducer {
//
//    @Autowired
//    private RabbitTemplate rabbitTemplate;
//
//    @GetMapping("/send")
//    public String sendMessage(@RequestParam String message, @RequestParam String consumer) {
//        String routingKey = "consumer.one"; // Default to consumer one
//
//        if ("two".equals(consumer)) {
//            routingKey = "consumer.two";
//        }
//        rabbitTemplate.convertAndSend(RabbitMQConfig.DIRECT_EXCHANGE , routingKey, message);
//        return "Message sent to consumer: " + consumer;
//    }
//}
