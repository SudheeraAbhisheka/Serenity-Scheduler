package org.example.servers_terminal.service;

import com.example.TaskObject;
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
    private final ExecutorService executor = Executors.newCachedThreadPool();
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

    public void runTimedHelloWorld(TextArea outputArea,
                                   int noOfThreads,
                                   int noOfTasks,
                                   int minWeight,
                                   int maxWeight,
                                   int minPriority,
                                   int maxPriority) {

        timedHelloWorld(outputArea,
                noOfThreads,
                noOfTasks,
                minWeight,
                maxWeight,
                minPriority,
                maxPriority);
    }


    private void timedHelloWorld(TextArea outputArea,
                                 int noOfThreads,
                                 int noOfTasks,
                                 int minWeight,
                                 int maxWeight,
                                 int minPriority,
                                 int maxPriority) {

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

//        scheduler = Executors.newScheduledThreadPool(noOfThreads + 1);
        Random random = new Random(12345);
        sendMessageCount.set(0);

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

        for (int tasksPerThreadCorrect : noOfTasksList) {
            executor.submit(() -> {
                Platform.runLater(() -> outputArea.appendText("Thread " + Thread.currentThread().getName() + " started.\n"));

                for (int i = 0; i < tasksPerThreadCorrect; i++) {
                    TaskObject taskObject = new TaskObject(
                            System.currentTimeMillis() + String.valueOf(Thread.currentThread().getId()),
                            1 + random.nextInt(10),
                            minWeight + random.nextInt(maxWeight - minWeight + 1),
                            false,
                            minPriority + random.nextInt(maxPriority - minPriority + 1),
                            System.currentTimeMillis()
                    );

                    if(!sendMessage_topic_1to10(taskObject)){
                        Platform.runLater(() -> outputArea.appendText("Failed to send: " + taskObject.getKey() + " \n"));
                    }
                    int currentCount = sendMessageCount.incrementAndGet();

                    if (currentCount == noOfTasks) {
                        Platform.runLater(() -> outputArea.appendText(currentCount + " tasks created.\n"));
                    }
                }
            });
        }
    }

    private boolean sendMessage_topic_1to10(TaskObject taskObject) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TaskObject> request = new HttpEntity<>(taskObject, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean generateReport(int count) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Integer> request = new HttpEntity<>(count, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:8084/api/set-no-of-tasks",
                    HttpMethod.POST, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

}
