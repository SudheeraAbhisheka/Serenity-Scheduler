package org.example.server1.service;

import com.example.AlgorithmRequestObj;
import com.example.KeyValueObject;
import com.example.ServerObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.component.RabbitMQ_consumer;
import org.springframework.beans.factory.annotation.Autowired;
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
    private BlockingQueue<String> dynamicBlockingQueue;

    private ExecutorService executorService;

    @Autowired
    public SchedulingAlgorithms(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.executorService = Executors.newCachedThreadPool();
    }

    public void setSchedulingAlgorithm(AlgorithmRequestObj algorithmRequestObj) {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        this.executorService = Executors.newCachedThreadPool();

        String url = "http://servers:8084/api/get-servers";
        ResponseEntity<LinkedHashMap> response = restTemplate.getForEntity(url, LinkedHashMap.class);
        LinkedHashMap<String, Double> servers = response.getBody();

        switch (algorithmRequestObj.getMessageBroker()) {
            case "kafka": {
                dynamicBlockingQueue = Kafka_consumer.getBlockingQueueCompleteF();
                break;
            }
            case "rabbitmq": {
                dynamicBlockingQueue = RabbitMQ_consumer.getBlockingQueueCompleteF();
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported messageBroker: " + algorithmRequestObj.getMessageBroker());
        }

        switch (algorithmRequestObj.getAlgorithm()) {
            case "complete-and-then-fetch":{
                for (String serverId : servers.keySet()) {
                    executorService.submit(() -> {
                        completeAndThenFetchModel(serverId);
                    });
                }
            }

                break;
            case "age-based-priority-scheduling":
                priorityBasedScheduling();
                break;
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithmRequestObj.getAlgorithm());
        }
    }

    public void completeAndThenFetchModel(String serverId) {
        ObjectMapper objectMapper = new ObjectMapper();

        while (!Thread.currentThread().isInterrupted()) {
            KeyValueObject keyValueObject;
            String message;

            try {
                message = dynamicBlockingQueue.take();
                keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                try {
                    String url = "http://servers:8084/api/server?serverId=" + serverId;
                    restTemplate.postForEntity(url, keyValueObject, String.class);
                } catch (Exception e) {
                    System.err.printf("Error sending to Server %s: %s\n", serverId, e.getMessage());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void priorityBasedScheduling() {
        Queue<ArrivedTimeObject> queuePriority1 = new ConcurrentLinkedQueue<>();
        Queue<ArrivedTimeObject> queuePriority2 = new ConcurrentLinkedQueue<>();
        Queue<ArrivedTimeObject> queuePriority3 = new ConcurrentLinkedQueue<>();
        final Object lock = new Object();

        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject = null;
                String message;

                try {
                    message = Kafka_consumer.getBlockingQueuePriorityS().take();

                    if (waitingThreads) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Exit the loop
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                if (keyValueObject != null) {
                    switch (keyValueObject.getPriority()) {
                        case 1 -> queuePriority1.add(new ArrivedTimeObject(System.currentTimeMillis(), keyValueObject));
                        case 2 -> queuePriority2.add(new ArrivedTimeObject(System.currentTimeMillis(), keyValueObject));
                        case 3 -> queuePriority3.add(new ArrivedTimeObject(System.currentTimeMillis(), keyValueObject));
                    }
                }
            }
        });

        executorService.submit(() -> {
            List<OldObject> oldObjects = new ArrayList<>();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (!queuePriority1.isEmpty()) {
                        long age = System.currentTimeMillis() - queuePriority1.peek().getArrivedTime();

                        if (age / 1000 > 10) {
                            oldObjects.add(new OldObject(age, queuePriority1.poll().getKeyValueObject()));
                        }

                    }
                    if (!queuePriority2.isEmpty()) {
                        long age = System.currentTimeMillis() - queuePriority2.peek().getArrivedTime();

                        if (age / 1000 > 15) {
                            oldObjects.add(new OldObject(age, queuePriority2.poll().getKeyValueObject()));
                        }

                    }
                    if (!queuePriority3.isEmpty()) {
                        long age = System.currentTimeMillis() - queuePriority3.peek().getArrivedTime();

                        if (age / 1000 > 20) {
                            oldObjects.add(new OldObject(age, queuePriority3.poll().getKeyValueObject()));
                        }

                    }

                    if (!oldObjects.isEmpty()) {
                        oldObjects.sort(Comparator.comparingLong(OldObject::getAge).reversed());
                        KeyValueObject k = oldObjects.remove(0).getKeyValueObject();
                        sendToServer2(k);
                    } else {
                        if (!queuePriority1.isEmpty()) {
                            sendToServer2(queuePriority1.poll().getKeyValueObject());
                        } else if (!queuePriority2.isEmpty()) {
                            sendToServer2(queuePriority2.poll().getKeyValueObject());
                        } else if (!queuePriority3.isEmpty()) {
                            sendToServer2(queuePriority3.poll().getKeyValueObject());
                        } else {
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

    public void addNewServersCATFModel(LinkedHashMap<String, Double> newServers){
        for (String serverId : newServers.keySet()) {
            executorService.submit(() -> {
                completeAndThenFetchModel(serverId);
            });
        }
    }
}
