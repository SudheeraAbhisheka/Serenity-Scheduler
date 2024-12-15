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

    public boolean setServers(LinkedHashMap<String, Double> servers){
        String url = "http://localhost:8084/api/set-servers";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<LinkedHashMap<String, Double>> request = new HttpEntity<>(servers, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Servers set successfully. " + servers.size());
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

    public boolean setNewServers(LinkedHashMap<String, Double> newServers){
        String url = "http://localhost:8084/api/set-new-servers";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<LinkedHashMap<String, Double>> request = new HttpEntity<>(newServers, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("New servers set successfully.");
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
        Random random = new Random();
        AtomicInteger value = new AtomicInteger();
        sendMessageCount.set(0);

        ArrayList<Integer> scheduleRate = new ArrayList<>();
        scheduleRate.add(5);
        scheduleRate.add(10);
        scheduleRate.add(15);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(scheduleRate.size());
        BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

        for (int milliSeconds : scheduleRate) {
            new Thread(() -> {
                int countLimit = 5;

                while (countLimit > 0){
                    value.set(1 + random.nextInt(10));

                    KeyValueObject keyValueObject = new KeyValueObject(
                            String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId(),
                            value.get(),
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
                        throw new RuntimeException(e);
                    }

                    countLimit--;
                }
            }).start();
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
                Platform.runLater(() -> outputArea.appendText(finalMessage1));

            }
        }).start();

//        scheduler.shutdown();
//        try {
//            scheduler.awaitTermination(20, TimeUnit.SECONDS);
//            Platform.runLater(() -> outputArea.appendText("sendMessage was called " + sendMessageCount.get() + " times.\n"));
//        } catch (InterruptedException e) {
//            Platform.runLater(() -> outputArea.appendText("Scheduler interrupted during awaitTermination.\n"));
//        }
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
