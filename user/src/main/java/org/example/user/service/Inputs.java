package org.example.user.service;

import com.example.KeyValueObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class Inputs {
    private final RestTemplate restTemplate;
    private final AtomicInteger sendMessageCount = new AtomicInteger(0);

    public Inputs(RestTemplate restTemplate){
        this.restTemplate = restTemplate;

        startTimedHelloWorld();
    }

    public void startTimedHelloWorld() {
        Scanner scanner = new Scanner(System.in);
        String userChoice;

        System.out.println("Do you want to run timedHelloWorld? (yes/no)");
        userChoice = scanner.nextLine();

        while (userChoice.equalsIgnoreCase("yes")) {
            timedHelloWorld();
            System.out.println("Do you want to run timedHelloWorld again? (yes/no)");
            userChoice = scanner.nextLine();
        }

        System.out.println("Program terminated.");

        //        ArrayList<Integer> randomSeconds = new ArrayList<>();
//
//        while (randomSeconds.size() < 3) {
//            int num = random.nextInt(60);
//            if (!randomSeconds.contains(num)) {
//                randomSeconds.add(num);
//            }
//        }
//
//        Collections.sort(randomSeconds);
    }

    public void timedHelloWorld() {
        Random random = new Random();
        AtomicInteger value = new AtomicInteger();

        sendMessageCount.set(0);



        ArrayList<Integer> randomSeconds = new ArrayList<>();
        randomSeconds.add(5);
        randomSeconds.add(5);
        randomSeconds.add(5);

        System.out.println("Random seconds (in ascending order): " + randomSeconds);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(randomSeconds.size());


        for (int seconds : randomSeconds) {
            int iterationTime = 4 + random.nextInt(2);

            scheduler.schedule(() -> {
                long printEndTime = System.currentTimeMillis() + iterationTime * 1000;
                while (System.currentTimeMillis() < printEndTime) {
//                    System.out.println("Hello world " + seconds + " from thread " + Thread.currentThread().getName());
                    value.set(1 + random.nextInt(20));

                    if(value.get() <= 10){
                        sendMessage_topic_1to10(new KeyValueObject(
                                String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId(),
                                value.get(),
                                1 + random.nextInt(10),
                                false
                                ));
                        System.out.println("Hello world " + seconds + " from thread " + Thread.currentThread().getName());
                    }else{
                        sendMessage_topic_11to21(new KeyValueObject(
                                String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId(),
                                value.get(),
                                1 + random.nextInt(10),
                                false));
                    }

                    sendMessageCount.incrementAndGet();


                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted during print loop");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, seconds, TimeUnit.SECONDS);
        }

        scheduler.shutdown();
        try {
            scheduler.awaitTermination(20, TimeUnit.SECONDS);
            System.out.println("sendMessage was called " + sendMessageCount.get() + " times.");
        } catch (InterruptedException e) {
            System.err.println("Scheduler interrupted during awaitTermination.");
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
                System.out.println("Message sent successfully: " + response.getBody());
            } else {
                System.out.println("Failed to send message. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMessage_topic_11to21(KeyValueObject keyValueObject) {
        String url = "http://localhost:8080/send-message/topic_11-21";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<KeyValueObject> request = new HttpEntity<>(keyValueObject, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Message sent successfully: " + response.getBody());
            } else {
                System.out.println("Failed to send message. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
