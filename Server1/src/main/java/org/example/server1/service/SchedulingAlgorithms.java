package org.example.server1.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.component.RabbitMQ_consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SchedulingAlgorithms {
    private final RestTemplate restTemplate;
    private boolean waitingThreads = false;
    @Getter
    private BlockingQueue<String> dynamicBlockingQueue;
    private ConcurrentLinkedQueue<String> wlbQueue;
    private BlockingQueue<String> blockingQueuePriorityS;

    private ExecutorService executorService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private LinkedHashMap<String, Double> servers;
    @Setter
    @Getter
    private String schedulingAlgorithm = "";
    @Getter
    private final BlockingQueue<KeyValueObject> crashedTasks = new LinkedBlockingQueue<>();
    @Getter
    private final ConcurrentMap<String, Future<?>> serverTaskMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, KeyValueObject> currentWorkingTask1 = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, Boolean> serverSwitches = new ConcurrentHashMap<>();

    @Autowired
    public SchedulingAlgorithms(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        executorService = Executors.newCachedThreadPool();
    }

    public void restarting() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        this.executorService = Executors.newCachedThreadPool();
    }

    public void setMessageBroker(String messageBroker){
        servers = fetchServers();
        switch (messageBroker) {
            case "kafka": {
                dynamicBlockingQueue = Kafka_consumer.getBlockingQueueCompleteF();
                wlbQueue = Kafka_consumer.getWlbQueue();
                blockingQueuePriorityS = Kafka_consumer.getBlockingQueuePriorityS();
                break;
            }
            case "rabbitmq": {
                dynamicBlockingQueue = RabbitMQ_consumer.getBlockingQueueCompleteF();
                wlbQueue = RabbitMQ_consumer.getWlbQueue();
                blockingQueuePriorityS = Kafka_consumer.getBlockingQueuePriorityS();
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported messageBroker: " + messageBroker);
        }
    }

    private LinkedHashMap<String, Double> fetchServers(){
        return restTemplate.exchange(
                "http://servers:8084/api/get-servers",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<LinkedHashMap<String, Double>>() {}
        ).getBody();
    }

    public void weightedLoadBalancing(int wlbFixedRate){
        scheduler.scheduleAtFixedRate(() -> {
            ArrayList<KeyValueObject> tasks = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();

            LinkedHashMap<String, Integer> remainingCaps = restTemplate.exchange(
                    "http://servers:8084/api/get-remaining-caps",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<LinkedHashMap<String, Integer>>() {}
            ).getBody();

            LinkedHashMap<String, Double> currentServerLoads = restTemplate.exchange(
                    "http://servers:8084/api/get-server-loads",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<LinkedHashMap<String, Double>>() {}
            ).getBody();

            String message;

            assert remainingCaps != null;
            int totalRemainingTime = remainingCaps.values().stream().mapToInt(Integer::intValue).sum();

            while ((wlbQueue.peek()) != null && totalRemainingTime > 0 ) {
                message = wlbQueue.poll();
                try {
                    KeyValueObject keyValueObject = objectMapper.readValue(message, KeyValueObject.class);
                    tasks.add(keyValueObject);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                totalRemainingTime--;
            }

            try {
                String url2 = "http://servers:8084/api/wlb-algorithm";
                restTemplate.postForEntity(url2, weightLoadBalancing(tasks, servers, remainingCaps, currentServerLoads), Map.class);
            } catch (Exception e) {
                System.err.printf("Error sending to wlb-algorithm: %s\n", e.getMessage());
            }

        }, 0, wlbFixedRate, TimeUnit.MILLISECONDS);
    }

    public void executeCATF(){
        for (String serverId : servers.keySet()) {
            if(!serverSwitches.containsKey(serverId)){
                Future<?> future = executorService.submit(() -> {completeAndThenFetchModel(serverId);});
                serverTaskMap.put(serverId, future);
                serverSwitches.put(serverId, true);
            }

        }

    }

    private void completeAndThenFetchModel(String serverId) {
        ObjectMapper objectMapper = new ObjectMapper();

        while (!Thread.currentThread().isInterrupted()) {
            KeyValueObject keyValueObject;
            String message;

            try {
                message = dynamicBlockingQueue.take();
                keyValueObject = objectMapper.readValue(message, KeyValueObject.class);
                currentWorkingTask1.put(serverId, keyValueObject);

                System.out.println(serverId + ": " + keyValueObject.getKey());

                String url = "http://servers:8084/api/assigning-to-servers?serverId=" + serverId;
                restTemplate.postForEntity(url, keyValueObject, String.class);
                currentWorkingTask1.remove(serverId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void priorityBasedScheduling(LinkedHashMap<Integer, Double> thresholdTime) {
        final Object lock = new Object();

        LinkedHashMap<Integer, Queue<ArrivedTimeObject>> queuePriorityX = new LinkedHashMap<>();

        for(Integer key : thresholdTime.keySet()) {
            queuePriorityX.put(key, new ConcurrentLinkedQueue<>());
        }

        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject = null;
                String message;

                try {
                    message = blockingQueuePriorityS.take();

                    if (waitingThreads) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                if (keyValueObject != null) {
                    queuePriorityX.get(keyValueObject.getPriority()).add(
                            new ArrivedTimeObject(System.currentTimeMillis(), keyValueObject)
                    );

                }
            }
        });

        executorService.submit(() -> {
            List<OldObject> oldObjects = new ArrayList<>();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    for (Map.Entry<Integer, Queue<ArrivedTimeObject>> entry : queuePriorityX.entrySet()) {
                        Integer priority = entry.getKey();
                        Queue<ArrivedTimeObject> priorityQueue = entry.getValue();

                        if (!priorityQueue.isEmpty()) {
                            long age = System.currentTimeMillis() - priorityQueue.peek().getArrivedTime();

                            if (age / 1000.0 > thresholdTime.get(priority)) {
                                oldObjects.add(new OldObject(age, priorityQueue.poll().getKeyValueObject()));
                            }

                        }
                    }

                    if (!oldObjects.isEmpty()) {
                        oldObjects.sort(Comparator.comparingLong(OldObject::getAge).reversed());
                        KeyValueObject k = oldObjects.remove(0).getKeyValueObject();
                        sendToServer2(k);

                    } else {
                        boolean priorityQueuesAreEmpty = true;

                        for(Queue<ArrivedTimeObject> priorityQueue : queuePriorityX.values()){
                            if(!priorityQueue.isEmpty()){
                                sendToServer2(priorityQueue.poll().getKeyValueObject());
                                priorityQueuesAreEmpty = false;

                                break;
                            }
                        }

                        if(priorityQueuesAreEmpty){
                            synchronized (lock) {
                                try {
                                    waitingThreads = true;
                                    lock.wait();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } finally {
                                    waitingThreads = false;
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void sendToServer2(KeyValueObject keyValueObject) {
        try {
            String url = "http://servers:8084/api/server2";
            restTemplate.postForEntity(url, keyValueObject, String.class);
        } catch (Exception e) {
            System.err.printf("Error sending to Server 2: %s\n", e.getMessage());
        }
    }

    public void notifyNewServersCATFModel(){
        servers = fetchServers();
        if(schedulingAlgorithm.equals("complete-and-then-fetch")){
            executeCATF();
        }
    }

    private Map<String, String> weightLoadBalancing(List<KeyValueObject> tasks, LinkedHashMap<String, Double> servers,
                                                   Map<String, Integer> remainingCaps, Map<String, Double> currentServerLoads) throws JsonProcessingException {
        Map<String, String> taskAssignments = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Double> serverLoads = new HashMap<>();
        for (String serverId : servers.keySet()) {
            serverLoads.put(
                    serverId,
                    currentServerLoads.getOrDefault(serverId, 0.0));
        }

        for (KeyValueObject task : tasks) {
            double taskWeight = task.getWeight();
            String bestServer = null;
            double bestCompletionTime = Double.MAX_VALUE;

            for (Map.Entry<String, Double> entry : servers.entrySet()) {
                String serverId = entry.getKey();

                if(remainingCaps.get(serverId) == 0){
                    continue;
                }

                double serverSpeed = entry.getValue();
                double currentLoad = serverLoads.get(serverId);

                double completionTime = currentLoad + (taskWeight / serverSpeed);
                if (completionTime < bestCompletionTime) {
                    bestCompletionTime = completionTime;
                    bestServer = serverId;
                }
            }

            double chosenServerSpeed = servers.get(bestServer);
            serverLoads.put(bestServer, serverLoads.get(bestServer) + (taskWeight / chosenServerSpeed));
            remainingCaps.put(bestServer, remainingCaps.get(bestServer) - 1);

            taskAssignments.put(objectMapper.writeValueAsString(task), bestServer);

        }
        return taskAssignments;
    }
}
