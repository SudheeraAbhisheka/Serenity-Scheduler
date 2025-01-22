package org.example.server1.service;

import com.example.KeyValueObject;
import lombok.Getter;
import org.example.server1.component.Kafka_consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SchedulingAlgorithms {
    private final RestTemplate restTemplate;
    private final Kafka_consumer kafka_consumer;
    private boolean waitingThreads = false;
    @Getter
    private BlockingQueue<KeyValueObject> dynamicBlockingQueue;
    private BlockingQueue<KeyValueObject> blockingQueuePriorityS;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    @Getter
    private final BlockingQueue<KeyValueObject> crashedTasks = new LinkedBlockingQueue<>();
    @Getter
    private final ConcurrentMap<String, Future<?>> serverTaskMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, KeyValueObject> currentWorkingTask = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, Boolean> serverSwitches = new ConcurrentHashMap<>();

    @Autowired
    public SchedulingAlgorithms(RestTemplate restTemplate, Kafka_consumer kafka_consumer) {
        this.restTemplate = restTemplate;
        this.kafka_consumer = kafka_consumer;
    }
    private String serverApiUrl;

    public void executeCATF(){
        dynamicBlockingQueue = kafka_consumer.getBlockingQueueCompleteF();

        LinkedHashMap<String, Double> servers = restTemplate.exchange(
                "http://servers:8084/api/get-servers",
//                serverApiUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<LinkedHashMap<String, Double>>() {}
        ).getBody();

        assert servers != null;
        for (String serverId : servers.keySet()) {
            if(!serverSwitches.containsKey(serverId)){
                System.out.println(serverId + " started");
                Future<?> future = executorService.submit(() -> {completeAndThenFetchModel(serverId);});
                serverTaskMap.put(serverId, future);
                serverSwitches.put(serverId, true);
            }

        }

    }

    private void completeAndThenFetchModel(String serverId) {
        while (!Thread.currentThread().isInterrupted()) {
            KeyValueObject keyValueObject;

            try {
                keyValueObject = dynamicBlockingQueue.take();
                currentWorkingTask.put(serverId, keyValueObject);

                String url = "http://servers:8084/api/assigning-to-servers?serverId=" + serverId;
                restTemplate.postForEntity(url, keyValueObject, String.class);
                currentWorkingTask.remove(serverId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void priorityBasedScheduling(LinkedHashMap<Integer, Double> thresholdTime) {
        final Object lock = new Object();
        ConcurrentHashMap<Integer, Queue<ArrivedTimeObject>> queuePriorityX = new ConcurrentHashMap<>();
        dynamicBlockingQueue = new LinkedBlockingQueue<>();
        blockingQueuePriorityS = kafka_consumer.getBlockingQueuePriorityS();

        for(Integer key : thresholdTime.keySet()) {
            queuePriorityX.put(key, new ConcurrentLinkedQueue<>());
        }

        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                KeyValueObject keyValueObject = null;

                try {
                    keyValueObject = blockingQueuePriorityS.take();

                    if (waitingThreads) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if(queuePriorityX.get(keyValueObject.getPriority()) != null){
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
                        dynamicBlockingQueue.add(k);

                    } else {
                        boolean priorityQueuesAreEmpty = true;

                        for(Queue<ArrivedTimeObject> priorityQueue : queuePriorityX.values()){
                            if(!priorityQueue.isEmpty()){
                                dynamicBlockingQueue.add(priorityQueue.poll().getKeyValueObject());
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
}