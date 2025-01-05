package org.example.servers_terminal.service;

import com.example.KeyValueObject;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MessageService {
    private final RestTemplate restTemplate;
    private final AtomicInteger sendMessageCount = new AtomicInteger(0);
    private String url;
    private ScheduledExecutorService scheduler;

    public MessageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setMessageBroker(String messageBroker){
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

    public void runTimedHelloWorld(TextArea outputArea, int noOfThreads, int noOfTasks) {
        new Thread(() -> {
            timedHelloWorld(outputArea, noOfThreads, noOfTasks);
        }).start();
    }

    private void timedHelloWorld(TextArea outputArea, int noOfThreads, int noOfTasks) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        ArrayList<Integer> noOfTasksList = new ArrayList<>();

        int tasksPerThread = noOfTasks / noOfThreads;
        int remainder = noOfTasks % noOfThreads;

        for (int i = 0; i < noOfThreads; i++) {
            if (i == noOfThreads - 1) {
                noOfTasksList.add(tasksPerThread + remainder);
            } else {
                noOfTasksList.add(tasksPerThread);
            }
        }

        scheduler = Executors.newScheduledThreadPool(noOfThreads + 1);
        BlockingQueue<String>   messageQueue = new LinkedBlockingQueue<>();
        Random random = new Random(12345);
        sendMessageCount.set(0);

        /* ******************************************************** */
        long startTime = System.currentTimeMillis();

        for (int tasksPerThread_correct : noOfTasksList) {
            scheduler.submit(() -> {

                System.out.println("started");

                for (int i = 0; i < tasksPerThread_correct; i++) {

                    KeyValueObject keyValueObject = new KeyValueObject(
                            String.valueOf(System.currentTimeMillis()) + Thread.currentThread().getId(),
                            1 + random.nextInt(10),
                            1 + random.nextInt(10),
                            false,
                            1 + random.nextInt(3),
                            System.currentTimeMillis()
                    );
                    sendMessage_topic_1to10(keyValueObject);
                    messageQueue.add("From thread " + Thread.currentThread().getName() + "\n" + keyValueObject + "\n");
//                    messageQueue.add(Integer.toString(sendMessageCount.get()));
                    sendMessageCount.incrementAndGet();

                }

                System.out.println((System.currentTimeMillis() - startTime)/1000.0);
                System.out.println(sendMessageCount);
            });
        }

        scheduler.scheduleAtFixedRate(() -> {
            try {
                ArrayList<String> batch = new ArrayList<>();
                messageQueue.drainTo(batch);

                if (!batch.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String msg : batch) {
                        sb.append(msg);
                    }

                    Platform.runLater(() -> {
                        outputArea.appendText(sb.toString());
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
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

}
