package org.example.kafka_consumer.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "my-topic", groupId = "my-group")
    public void listen(String message) {
        System.out.println("Received message from Kafka: " + message);
    }

//    public static void HelloWorldRepeater() {
//        int iterationTime = 5;
//        int waitTime = 10;
//        int totalDuration = 70;
//        long startTime = System.currentTimeMillis();
//        int count = 0;
//
//        while ((System.currentTimeMillis() - startTime) < totalDuration * 1000) {
//            // Print "Hello world" every 0.1 seconds for 5 seconds
//            long printEndTime = System.currentTimeMillis() + iterationTime * 1000;
//            while (System.currentTimeMillis() < printEndTime) {
//                System.out.println("Hello world " + count);
//                try {
//                    Thread.sleep(100); // 0.1 second delay
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            count++;
//
//            // Wait for 10 seconds before next iteration
//            try {
//                Thread.sleep(waitTime * 1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}