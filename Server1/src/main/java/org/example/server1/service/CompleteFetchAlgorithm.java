package org.example.server1.service;

import com.example.TaskObject;
import lombok.Getter;
import lombok.Setter;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.controller.ServerControllerEmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CompleteFetchAlgorithm {
    private final RestTemplate restTemplate;
    @Getter
    @Setter
    private BlockingQueue<TaskObject> dynamicBlockingQueue;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    @Getter
    private final BlockingQueue<TaskObject> crashedTasks = new LinkedBlockingQueue<>();
    @Getter
    private final ConcurrentMap<String, Future<?>> serverTaskMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TaskObject> currentWorkingTask = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, Boolean> runningServers = new ConcurrentHashMap<>();
    private final ServerControllerEmitter serverControllerEmitter;
    private final Kafka_consumer kafka_consumer;

    @Autowired
    public CompleteFetchAlgorithm(RestTemplate restTemplate, Kafka_consumer kafka_consumer, ServerControllerEmitter serverControllerEmitter) {
        this.restTemplate = restTemplate;
        this.kafka_consumer = kafka_consumer;
        this.serverControllerEmitter = serverControllerEmitter;
    }

    public void executeCATF(){
        LinkedHashMap<String, Double> servers = restTemplate.exchange(
                "http://servers:8084/api/get-servers",
//                serverApiUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<LinkedHashMap<String, Double>>() {}
        ).getBody();

        assert servers != null;
        for (String serverId : servers.keySet()) {
            if(!runningServers.containsKey(serverId)){
                Future<?> future = executorService.submit(() -> {completeAndThenFetchModel(serverId);});
                serverTaskMap.put(serverId, future);
                runningServers.put(serverId, true);
            }

        }

    }

    private void completeAndThenFetchModel(String serverId) {
        while (!Thread.currentThread().isInterrupted()) {
            TaskObject taskObject;

            try {
                taskObject = dynamicBlockingQueue.take();
                currentWorkingTask.put(serverId, taskObject);

                String url = "http://servers:8084/api/assigning-to-servers?serverId=" + serverId;
                restTemplate.postForEntity(url, taskObject, String.class);
                currentWorkingTask.remove(serverId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void terminateServer(String serverId, List<TaskObject> crashedTasks, String algorithmName) throws InterruptedException {

        String message = "Crashed server: " + serverId + "\n";

        if(currentWorkingTask.containsKey(serverId)){
            crashedTasks.add(
                    currentWorkingTask.remove(serverId)
            );
        }

        message += "Number of tasks replaced: " + crashedTasks.size() + "\n";

        Future<?> future = serverTaskMap.get(serverId);
        boolean successful;
        if (serverTaskMap.containsKey(serverId)) {
            successful = future.cancel(true);
            if(successful) {
                serverTaskMap.remove(serverId);
            }
            else{
                System.out.println("Failed to crash server " + serverId);
            }
        }

        runningServers.put(serverId, false);
        kafka_consumer.setCrashedTasks(true);

        switch(algorithmName){
            case "complete-and-then-fetch": {
                for(TaskObject task : crashedTasks) {
                    kafka_consumer.getBlockingQueueCompleteF().put(task);
                }

                break;
            }

            case "age-based-priority-scheduling": {
                for(TaskObject task : crashedTasks) {
                    kafka_consumer.getBlockingQueuePriorityS().add(task);
                }
                break;
            }

            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithmName);

        }

        serverControllerEmitter.sendUpdate(message);

        kafka_consumer.setCrashedTasks(false);

        Object lock = kafka_consumer.getLock();

        synchronized (lock) {
            lock.notify();
        }

    }
}

