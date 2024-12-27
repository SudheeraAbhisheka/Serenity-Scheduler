package org.example.server1.service;

import com.example.AlgorithmRequestObj;
import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.component.RabbitMQ_consumer;
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
public class SchedulingAlgorithms {
    private final RestTemplate restTemplate;
    private boolean waitingThreads = false;

    private BlockingQueue<String> dynamicBlockingQueue;
    private ConcurrentLinkedQueue<String> wlbQueue;
    private BlockingQueue<String> blockingQueuePriorityS;

    private final LinkedHashMap<String, Boolean> runningServers;

    private ExecutorService executorService;
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private LinkedHashMap<String, Double> servers;

    @Autowired
    public SchedulingAlgorithms(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        executorService = Executors.newCachedThreadPool();
        runningServers = new LinkedHashMap<>();
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
            if(!runningServers.containsKey(serverId)){
                executorService.submit(() -> {completeAndThenFetchModel(serverId);});
                runningServers.put(serverId, null);
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
    }

/*
    public HashMap<String, String> weightLoadBalancing(List<KeyValueObject> tasks, LinkedHashMap<String, Double> servers) throws JsonProcessingException {
        // Greedy method
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String, String> taskAssignments = new HashMap<>();
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
            taskAssignments.put( objectMapper.writeValueAsString(task), bestServer);
            serverLoads.put(bestServer, bestLoadAfterAssignment);
        }

        double[] totalCompletionTime = {0.0};

//        taskAssignments.forEach((task, serverId) -> {
//            double completionTime = task.getWeight() / servers.get(serverId);
//            System.out.println("serverId: " + serverId + " taskWeight: " + task.getWeight()
//                    + " completionTime: " + completionTime);
//            totalCompletionTime[0] += completionTime;
//        });

//        System.out.println("Total completion time: " + totalCompletionTime[0]);

        return taskAssignments;
    }
*/

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


/*
    public Map<KeyValueObject, String> weightLoadBalancing(ArrayList<KeyValueObject> keyValueObjects, LinkedHashMap<String, Double> servers){
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
            String selectedServerId = wLBArray[taskId][0].getServerId();
            KeyValueObject leastTimeObj = wLBArray[taskId][0].getKeyValueObject();
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

        return taskServersMap;

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
