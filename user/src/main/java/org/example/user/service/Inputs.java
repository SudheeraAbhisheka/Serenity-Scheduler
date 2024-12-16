package org.example.user.service;

import com.example.AlgorithmRequestObj;
import com.example.KeyValueObject;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class Inputs {
    private final RestTemplate restTemplate;
    private final AtomicInteger sendMessageCount = new AtomicInteger(0);
    private String url;
    private ScheduledExecutorService scheduler;

    public Inputs(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void runTimedHelloWorld(TextArea outputArea){
        new Thread(() -> {
            timedHelloWorld(outputArea);
        }).start();
    }

    public boolean setAlgorithm(String algorithm, String messageBroker){
        setMessageBroker(messageBroker);
        String url = "http://localhost:8083/consumer-one/set-algorithm";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        AlgorithmRequestObj requestBody = new AlgorithmRequestObj(algorithm, messageBroker);

        HttpEntity<AlgorithmRequestObj> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Algorithm set successfully.");
                setSuccess = true;
            } else {
                System.out.println("Failed to set algorithm. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while setting algorithm: " + e.getMessage());
            e.printStackTrace();
        }

        return setSuccess;
    }

    private void setMessageBroker(String messageBroker){
        switch(messageBroker){
            case "kafka" : {
                url = "http://localhost:8080/kafka-server/topic_1-10";
                break;
            }
            case "rabbitmq" : {
                url = "http://localhost:8080/rebbitmq-server/topic_1-10";
                break;
            }
            default: url = "http://localhost:8080/kafka-server/topic_1-10";
        }
    }

    private void timedHelloWorld(TextArea outputArea) {
        // If there's already a scheduler running, shut it down
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        // Create a new scheduler
        ArrayList<Integer> scheduleRate = new ArrayList<>();
        scheduleRate.add(5);
        scheduleRate.add(10);
        scheduleRate.add(15);

        scheduler = Executors.newScheduledThreadPool(scheduleRate.size() + 1);
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
        Random random = new Random(12345);
        sendMessageCount.set(0);

        // Submit producer tasks
        for (int milliSeconds : scheduleRate) {
            scheduler.submit(() -> {
                int countLimit = 5;
                while (countLimit > 0) {
                    KeyValueObject keyValueObject = new KeyValueObject(
                            String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId(),
                            1 + random.nextInt(10),
                            1 + random.nextInt(10),
                            false,
                            1 + random.nextInt(3)
                    );
                    sendMessage_topic_1to10(keyValueObject);
                    messageQueue.add("From thread " + Thread.currentThread().getName() + "\n" + keyValueObject + "\n");
                    sendMessageCount.incrementAndGet();

                    try {
                        Thread.sleep(milliSeconds);
                    } catch (InterruptedException e) {
                        // Task interrupted: break out
                        Thread.currentThread().interrupt();
                        break;
                    }

                    countLimit--;
                }
            });
        }

        // Submit the consumer task
        scheduler.submit(() -> {
            try {
                while (true) {
                    String finalMessage = messageQueue.take();
                    Platform.runLater(() -> outputArea.appendText(finalMessage));
                }
            } catch (InterruptedException e) {
                // Task interrupted, exit loop
                Thread.currentThread().interrupt();
            }
        });
    }


    private void sendMessage_topic_1to10(KeyValueObject keyValueObject) {
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
