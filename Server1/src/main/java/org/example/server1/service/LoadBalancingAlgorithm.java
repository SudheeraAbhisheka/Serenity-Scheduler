package org.example.server1.service;

import com.example.KeyValueObject;
import lombok.Getter;
import lombok.Setter;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.controller.ServerControllerEmitter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

@Service
public class LoadBalancingAlgorithm {
    private final Kafka_consumer kafka_consumer;
    private final RestTemplate restTemplate;
    @Getter
    @Setter
    private BlockingQueue<KeyValueObject> wlbQueue;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, BlockingQueue<KeyValueObject>> queuesForServers = new ConcurrentHashMap<>();
    private final ServerControllerEmitter serverControllerEmitter;
    @Getter
    private final ConcurrentMap<String, Future<?>> serverTaskMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, Boolean> runningServers = new ConcurrentHashMap<>();
    private LinkedHashMap<String, Double> servers;
    @Setter
    private boolean isEmptyServerAvailable = false;
    @Getter
    private final Object lock = new Object();
    private final Object secondLock = new Object();
    private final ConcurrentHashMap<String, KeyValueObject> currentWorkingTask = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Queue<KeyValueObject>> currentWorkingTasks = new ConcurrentHashMap<>();
    private long taskCameAt;
    KeyValueObject taskForNextIteration = null;
    private long arrivedTime;
    private boolean runningThread = false;

    public LoadBalancingAlgorithm(RestTemplate restTemplate, Kafka_consumer kafka_consumer, ServerControllerEmitter serverControllerEmitter) {
        this.restTemplate = restTemplate;
        this.kafka_consumer = kafka_consumer;
        this.serverControllerEmitter = serverControllerEmitter;
    }

    public void wlb_serverInit(){
        servers = restTemplate.exchange(
                "http://servers:8084/api/get-servers",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<LinkedHashMap<String, Double>>() {}
        ).getBody();

        assert servers != null;
        for (String serverId : servers.keySet()) {
            if(!runningServers.containsKey(serverId)){
                System.out.println(serverId + " started");
                queuesForServers.put(serverId, new LinkedBlockingQueue<>());
                Future<?> future = executorService.submit(() -> { sendToServers(serverId, queuesForServers.get(serverId));});
                serverTaskMap.put(serverId, future);
                runningServers.put(serverId, true);
            }

        }
    }

    ArrayList<KeyValueObject> tasks;
    int rCap;

    public void weightedLoadBalancing()  {
        executorService.submit(()->{
            while (!Thread.currentThread().isInterrupted()) {
                tasks = new ArrayList<>();

                if(!isEmptyServerAvailable){
                    synchronized(lock){
                        try {
                            System.out.println("Waiting for empty server");
                            lock.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                isEmptyServerAvailable = false;

                if(taskForNextIteration != null){
                    tasks.add(taskForNextIteration);
                    arrivedTime = System.currentTimeMillis();
                    taskForNextIteration = null;

                }

                Integer rCapObject = restTemplate.exchange(
                        "http://servers:8084/api/total-servers-capacity",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Integer>() {}
                ).getBody();

                rCap = (rCapObject != null) ? rCapObject : 0;

                if(tasks == null){
                    System.out.println("caught you");
                }
                else{
                    rCap -= tasks.size();
                }

                for(int i = rCap; i > 0; i--) {
                    KeyValueObject task;

                    try {
                        if(runningThread){
                            synchronized (secondLock){
                                System.out.println("locking..");
                                secondLock.wait();
                                System.out.println("unlocking....");
                            }
                        }

                        task = wlbQueue.take();

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    arrivedTime = System.currentTimeMillis();

                    if(tasks == null){
                        taskForNextIteration = task;
                        break;
                    }else{
                        tasks.add(task);
                    }
                }

                if(tasks != null){
                    weightLoadBalancing(tasks, servers);
                    System.out.println("tasks size 1(after): " + tasks.size());
                }

            }
        });
        executorService.submit(()->{
            long lastlyUnlockedFor = Long.MAX_VALUE;
            long indicator = 0;

            while(!Thread.currentThread().isInterrupted()){
                long currentTime = System.currentTimeMillis();
                long timeDifferance = currentTime - arrivedTime;
                final long THREAD_SLEEP_TIME = 500;

                if(timeDifferance >= 500){
                    if (tasks != null && !tasks.isEmpty() && arrivedTime != lastlyUnlockedFor) {
                        runningThread = true;
                        weightLoadBalancing(tasks, servers);
                        System.out.println("tasks size 2(after): " + tasks.size());
                        tasks = null;
                        runningThread = false;
                        synchronized (secondLock){
                            secondLock.notify();
                        }
                        lastlyUnlockedFor = arrivedTime;
                        indicator = 0;
                    }
                }
                else {
                    indicator++;

                    if(THREAD_SLEEP_TIME * indicator > 5000){
                        if (tasks != null && !tasks.isEmpty()) {
                            runningThread = true;
                            weightLoadBalancing(tasks, servers);
                            System.out.println("tasks size 3(after): " + tasks.size());
                            tasks = null;
                            runningThread = false;
                            synchronized (secondLock){
                                secondLock.notify();
                            }
                            lastlyUnlockedFor = arrivedTime;
                        }

                        indicator = 0;
                    }
                }

                try {
                    Thread.sleep(THREAD_SLEEP_TIME);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void weightLoadBalancing(List<KeyValueObject> tasks, LinkedHashMap<String, Double> servers) {
        Map<String, Double> serverLoads = new HashMap<>();
        double currentServerLoad = 0.0;

        LinkedHashMap<String, Double> currentServerLoads = restTemplate.exchange(
                "http://servers:8084/api/get-server-loads",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<LinkedHashMap<String, Double>>() {}
        ).getBody();

        for (String serverId : servers.keySet()) {
            if(currentServerLoads != null){
                currentServerLoad = currentServerLoads.getOrDefault(serverId, 0.0);
            }

            serverLoads.put(serverId, currentServerLoad);
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

            queuesForServers.get(bestServer).add(task);
        }
    }

    private void sendToServers(String serverId, BlockingQueue<KeyValueObject> wlb_serverQueue) {
        while (!Thread.currentThread().isInterrupted()) {
            KeyValueObject keyValueObject;

            try {
                keyValueObject = wlb_serverQueue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            currentWorkingTask.put(serverId, keyValueObject);
            if(currentWorkingTasks.containsKey(serverId)){
                currentWorkingTasks.get(serverId).add(keyValueObject);
            }
            else{
                currentWorkingTasks.put(serverId, new LinkedList<>());
                currentWorkingTasks.get(serverId).add(keyValueObject);
            }

            String url = "http://servers:8084/api/assigning-to-servers?serverId=" + serverId;
            restTemplate.postForEntity(url, keyValueObject, String.class);

            currentWorkingTask.remove(serverId);
            currentWorkingTasks.get(serverId).remove(keyValueObject);

        }
    }

    public void terminateServer(String serverId, List<KeyValueObject> crashedTasks, String algorithmName) throws InterruptedException {
        String message = "Crashed server: " + serverId + "\n";

        servers = restTemplate.exchange(
                "http://servers:8084/api/get-servers",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<LinkedHashMap<String, Double>>() {}
        ).getBody();

        if(currentWorkingTask.containsKey(serverId)){
            crashedTasks.add(
                    currentWorkingTask.remove(serverId)
            );
            System.out.println("current working task: "+ 1);
        }

        System.out.println("crashed tasks: " + crashedTasks.size());
        if(currentWorkingTasks.containsKey(serverId)){
            System.out.println("current working tasks: "+ currentWorkingTasks.get(serverId).size());
//            crashedTasks.addAll(currentWorkingTasks.get(serverId));
        }

        if(queuesForServers.containsKey(serverId)){
            System.out.println("queues for server: "+ queuesForServers.get(serverId).size());
            BlockingQueue<KeyValueObject> queue = queuesForServers.get(serverId);
            crashedTasks.addAll(queue);
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
            case "age-based-priority-scheduling": {
                for(KeyValueObject task : crashedTasks) {
                    kafka_consumer.getBlockingQueuePriorityS().add(task);
                }
                break;
            }

            case "weight-load-balancing" :
                for(KeyValueObject task : crashedTasks) {
                    kafka_consumer.getWlbQueue().add(task);
                }
                break;

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
