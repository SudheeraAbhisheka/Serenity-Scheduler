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

import java.security.Key;
import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.awt.AWTEventMulticaster.add;

@Service
public class SchedulingAlgorithms {
    private final RestTemplate restTemplate;
    private boolean waitingThreads = false;
    private BlockingQueue<String> dynamicBlockingQueue;

    private ExecutorService executorService;
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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

            case "weight-load-balancing":{
                ConcurrentLinkedQueue<String> wlbQueue = Kafka_consumer.getWlbQueue();

                scheduler.scheduleAtFixedRate(() -> {
                    ArrayList<Double> taskWeights = new ArrayList<>();
                    ArrayList<KeyValueObject> tasks = new ArrayList<>();
                    ObjectMapper objectMapper = new ObjectMapper();

                    String message;
                    while ((message = wlbQueue.poll()) != null) {
                        try {
                            KeyValueObject keyValueObject = objectMapper.readValue(message, KeyValueObject.class);
                            tasks.add(keyValueObject);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    weightLoadBalancing(tasks, servers);

                }, 3000, 10000, TimeUnit.MILLISECONDS);
            }

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

    public Map<KeyValueObject, String> weightLoadBalancing(List<KeyValueObject> tasks, LinkedHashMap<String, Double> servers) {
        Map<KeyValueObject, String> taskAssignments = new HashMap<>();
        tasks.sort((t1, t2) -> Double.compare(t2.getWeight(), t1.getWeight()));
        Map<String, Double> serverLoads = new HashMap<>();

        for (String serverId : servers.keySet()) {
            serverLoads.put(serverId, 0.0);
        }

        for (KeyValueObject task : tasks) {
            double taskWeight = task.getWeight();
            String bestServer = null;
            double bestLoadAfterAssignment = Double.MAX_VALUE;
            for (Map.Entry<String, Double> entry : serverLoads.entrySet()) {
                String serverId = entry.getKey();
                double currentLoad = entry.getValue();
                double serverSpeed = servers.get(serverId);
                double taskTime = taskWeight / serverSpeed;
                double newLoad = currentLoad + taskTime;
                if (newLoad < bestLoadAfterAssignment) {
                    bestLoadAfterAssignment = newLoad;
                    bestServer = serverId;
                }
            }
            taskAssignments.put(task, bestServer);
            serverLoads.put(bestServer, bestLoadAfterAssignment);
        }

        taskAssignments.forEach((task, serverId) ->
                System.out.println("serverId: " + serverId + " taskWeight: " + task.getWeight()
                        + " completionTime: " + (task.getWeight() / servers.get(serverId))));

        return taskAssignments;
    }


/*    public void weightLoadBalancing(List<KeyValueObject> tasks, LinkedHashMap<String, Double> servers) {
        Map<KeyValueObject, String> taskAssignments = new HashMap<>();

        Map<String, Double> serverLoads = new HashMap<>();
        for (String serverId : servers.keySet()) {
            serverLoads.put(serverId, 0.0);
        }

        for (KeyValueObject task : tasks) {
            double taskWeight = task.getWeight();
            String bestServer = null;
            double bestCompletionTime = Double.MAX_VALUE;

            for (Map.Entry<String, Double> entry : servers.entrySet()) {
                String serverId = entry.getKey();
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

            taskAssignments.put(task, bestServer);
        }

        taskAssignments.forEach((task, serverId) ->
                System.out.println("serverId: " + serverId + " weight: " + task.getWeight()));
    }*/


/*
    public void weightLoadBalancing(ArrayList<KeyValueObject> keyValueObjects, LinkedHashMap<String, Double> servers){
        Map<KeyValueObject, String> taskServersMap = new HashMap<>();

        WightedLBObject[][] wLBArray;

        wLBArray = new WightedLBObject[keyValueObjects.size()][servers.size()];

        int i = 0;

        for(KeyValueObject keyValueObject : keyValueObjects){
            int j = 0;
            for (Map.Entry<String, Double> entry : servers.entrySet()) {
                String serverId = entry.getKey();
                Double serverSpeed = entry.getValue();

                wLBArray[i][j] = new WightedLBObject(serverId, serverSpeed, keyValueObject);
                wLBArray[i][j].setTime(keyValueObject.getWeight() / serverSpeed);
                j++;
            }
            i++;
        }

        for (int taskId = 0; taskId < wLBArray.length; taskId++) {
            double leastTime = wLBArray[taskId][0].getTime();
            String selectedServerId = "1";
            KeyValueObject leastTimeObj = null;
            int j = 0;
            int selectedJ = 0;

            for (int serverId = 1; serverId < wLBArray[taskId].length; serverId++) {
                if(wLBArray[taskId][serverId].getTime() < leastTime){
                    leastTime = wLBArray[taskId][serverId].getTime();
                    selectedServerId = wLBArray[taskId][serverId].getServerId();
                    leastTimeObj = wLBArray[taskId][serverId].getKeyValueObject();
                    selectedJ = j;
                }
                j++;
            }

            taskServersMap.put(leastTimeObj, selectedServerId);

            if (taskId + 1 < wLBArray.length) {
                for (int k = taskId + 1; k < keyValueObjects.size(); k++) {
                    wLBArray[k][selectedJ].setTime(
                            wLBArray[k][selectedJ].getTime() +
                            wLBArray[taskId][selectedJ].getTime()
                    );
                }
            }
        }

    }
*/

    /*public void weightedLoadBalancing(){
        ArrayList<Double> taskWeight = new ArrayList<>(Arrays.asList(8.0, 7.0, 6.0, 5.0, 4.0));
        ArrayList<Double> serverSpeeds = new ArrayList<>(Arrays.asList(10.0, 30.0, 20.0, 15.0));
        Map<String, String> taskServersMap = new HashMap<>();
        double[][] twoDArray = new double[taskWeight.size()][serverSpeeds.size()];

        for (int i = 0; i < taskWeight.size(); i++) {
            for (int j = 0; j < serverSpeeds.size(); j++) {
                twoDArray[i][j] = taskWeight.get(i)/serverSpeeds.get(j);
            }
        }

        for (int taskId = 0; taskId < twoDArray.length; taskId++) {
            double leastTime = twoDArray[taskId][0];
            int selectedServerId = 0;

            for (int serverId = 1; serverId < twoDArray[taskId].length; serverId++) {
                if(twoDArray[taskId][serverId] < leastTime){
                    leastTime = twoDArray[taskId][serverId];
                    selectedServerId = serverId;
                }
            }

            taskServersMap.put(String.valueOf(taskId), String.valueOf(selectedServerId));

//            System.out.println("task id = " + taskId);
//            System.out.println("server id = " + selectedServerId);
//            System.out.println("weight = " + leastTime);

            if (taskId + 1 < twoDArray.length) {
                for (int i = taskId + 1; i < taskWeight.size(); i++) {
                    twoDArray[i][selectedServerId] += twoDArray[taskId][selectedServerId];

                }
            }


//
//            for (int i = 0; i < twoDArray.length; i++) {
//                for (int j = 0; j < twoDArray[i].length; j++) {
//                    twoDArray[i][j] = Math.round(twoDArray[i][j] * 100.0) / 100.0;
//                    System.out.print(twoDArray[i][j] + " ");
//
//                }
//                System.out.println();
//            }
//
//            System.out.println();
        }

    }*/
}
