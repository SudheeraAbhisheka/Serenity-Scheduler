package org.example.user.service;

import com.example.KeyValueObject;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class Inputs {
    private final RestTemplate restTemplate;
    private final AtomicInteger sendMessageCount = new AtomicInteger(0);

    public Inputs(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void runTimedHelloWorld(TextArea outputArea){
        new Thread(() -> {
            timedHelloWorld(outputArea);
        }).start();
    }

    private void timedHelloWorld(TextArea outputArea) {
        Random random = new Random(); //ThreadLocalRandom
        AtomicInteger value = new AtomicInteger();
        sendMessageCount.set(0);

        ArrayList<Integer> randomSeconds = new ArrayList<>();
        randomSeconds.add(5);
        randomSeconds.add(5);
        randomSeconds.add(5);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(randomSeconds.size());
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

        for (int seconds : randomSeconds) {
            int iterationTime = 4 + random.nextInt(2);

            scheduler.schedule(() -> {
                long printEndTime = System.currentTimeMillis() + iterationTime * 1000;
                while (System.currentTimeMillis() < printEndTime) {
                    value.set(1 + random.nextInt(20));

                    if (value.get() <= 10) {
                        KeyValueObject keyValueObject = new KeyValueObject(
                                String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId(),
                                value.get(),
                                1 + random.nextInt(10),
                                false
                        );
                        sendMessage_topic_1to10(keyValueObject);
//                        Platform.runLater(() -> outputArea.appendText(
//                                " from thread " + Thread.currentThread().getName() + "\n" + keyValueObject + "\n"
//                        ));
//                        outputArea.appendText(" from thread " + Thread.currentThread().getName() + "\n"
//                        + keyValueObject + "\n");
                        messageQueue.add("From thread " + Thread.currentThread().getName() + "\n" + keyValueObject + "\n");
                        sendMessageCount.incrementAndGet();
                    } else {
//                        sendMessage_topic_11to20(new KeyValueObject(
//                                String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId(),
//                                value.get(),
//                                1 + random.nextInt(10),
//                                false));
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Platform.runLater(() -> outputArea.appendText("Interrupted during print loop\n"));
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, seconds, TimeUnit.SECONDS);
        }

        new Thread(() -> {
            String finalMessage = null;
            while (true) {
                try {
                    finalMessage = messageQueue.take();

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                String finalMessage1 = finalMessage;
                outputArea.appendText(finalMessage1);
//                Platform.runLater(() -> outputArea.appendText(finalMessage1));

            }
        }).start();

        scheduler.shutdown();
        try {
            scheduler.awaitTermination(20, TimeUnit.SECONDS);
            outputArea.appendText("sendMessage was called " + sendMessageCount.get() + " times.\n");
        } catch (InterruptedException e) {
            outputArea.appendText("Scheduler interrupted during awaitTermination.\n");
        }
    }



    private void sendMessage_topic_1to10(KeyValueObject keyValueObject) {
        String url = "http://localhost:8080/send-message/topic_1-10";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<KeyValueObject> request = new HttpEntity<>(keyValueObject, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {

            } else {
                System.out.println("Failed to send message. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMessage_topic_11to20(KeyValueObject keyValueObject) {
        String url = "http://localhost:8080/send-message/topic_11-20";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<KeyValueObject> request = new HttpEntity<>(keyValueObject, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {

            } else {
                System.out.println("Failed to send message. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
