package org.example.user.service;

import com.example.KeyValueObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class Inputs {
    private final RestTemplate restTemplate;

    public Inputs(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
        sendMessage(takingInputs());

//        timedHelloWorld();
    }

    public KeyValueObject takingInputs(){
        Scanner scanner = new Scanner(System.in);

        System.out.println("Type 1 & press Enter to upload");
        scanner.nextInt();


        System.out.println("Done");

        return new KeyValueObject(String.valueOf(System.currentTimeMillis()), 8);

    }

    public void timedHelloWorld() {
        Random random = new Random();
        ArrayList<Integer> randomSeconds = new ArrayList<>();

        while (randomSeconds.size() < 3) {
            int num = random.nextInt(60);
            if (!randomSeconds.contains(num)) {
                randomSeconds.add(num);
            }
        }

        Collections.sort(randomSeconds);
        System.out.println("Random seconds (in ascending order): " + randomSeconds);

        long startTime = System.currentTimeMillis();

        for (int seconds : randomSeconds) {
            long waitTime = (seconds * 1000) - (System.currentTimeMillis() - startTime);

            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    System.err.println("The thread was interrupted while waiting.");
                    Thread.currentThread().interrupt(); // Restore the interrupted status
                    return;
                }
            }

            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            int iterationTime = 4 + random.nextInt(2);;

            scheduler.schedule(() -> {
                long printEndTime = System.currentTimeMillis() + iterationTime * 1000;
                while (System.currentTimeMillis() < printEndTime) {
                    System.out.println("Hello world " + seconds);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted during print loop");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, seconds, TimeUnit.SECONDS);

            scheduler.shutdown();

        }
    }


    private void sendMessage(KeyValueObject keyValueObject) {
        String url = "http://localhost:8080/send-message";
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
