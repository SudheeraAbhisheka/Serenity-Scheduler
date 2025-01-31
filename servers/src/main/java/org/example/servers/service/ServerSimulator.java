package org.example.servers.service;

import com.example.KeyValueObject;
import com.example.ServerObject;
import lombok.Getter;
import lombok.Setter;
import org.example.servers.controller.ServerControllerEmitter;
import org.example.servers.controller.ServerDetailsController;
import org.example.servers.entity.KeyValueObjectEntity;
import org.example.servers.exception.CustomDatabaseException;
import org.example.servers.repository.KeyValueObjectRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ServerSimulator {
    @Getter
    @Setter
    private ConcurrentHashMap<String, ServerObject> servers;
    private final LinkedHashMap<String, Boolean> runningServers = new LinkedHashMap<>();
    private final ConcurrentHashMap<String, Integer> aliveServers = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    @Getter
    private AtomicInteger atomicCount = new AtomicInteger();
    ServerControllerEmitter emitter;
    private final AtomicLong atomicTotalWait = new AtomicLong(0);
    @Getter
    private final ConcurrentHashMap<String, Integer> taskCompletion = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<String, Future<?>> serverTaskMap = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, KeyValueObject> currentWorkingTasks = new ConcurrentHashMap<>();
    @Setter
    private int checkingHeartBeatIntervals = 3000;
    @Setter
    private int makingHeartBeatIntervals = 1000;
    private final ServerDetailsController serverDetailsController;
    private final Map<String, Map<String, Double>> serversDetails = new LinkedHashMap<>();
    private final Map<String, Integer> serversLoad = new ConcurrentHashMap<>();
    private final StringBuffer sb = new StringBuffer();
    private final KeyValueObjectRepository keyValueObjectRepository;

    public ServerSimulator(ServerControllerEmitter emitter, RestTemplate restTemplate,
                           ServerDetailsController serverDetailsController, KeyValueObjectRepository keyValueObjectRepository ) {
        this.emitter = emitter;
        this.restTemplate = restTemplate;
        this.serverDetailsController = serverDetailsController;
        this.keyValueObjectRepository = keyValueObjectRepository;


        executorService.submit(() -> {
            int idealHeartBeat = checkingHeartBeatIntervals/makingHeartBeatIntervals - 1;
            if(idealHeartBeat == 0){
                idealHeartBeat = 1;
            }

            while (true) {
                checkingHeartBeat(idealHeartBeat);
                try {
                    Thread.sleep(checkingHeartBeatIntervals);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        executorService.submit(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                serverDetailsController.sendServerInit(serversDetails);

                if(servers != null){
                    for(ServerObject server : servers.values()){
                        if(server.getQueueServer().isEmpty()){
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            serverDetailsController.sendServerDetails(server.getServerId(), server.getQueueServer().size());
                            serversLoad.put(server.getServerId(), server.getQueueServer().size());
                        }
                    }
                }

                serverDetailsController.sendTaskCompletion(taskCompletion);
                serverDetailsController.sendServerDetails_(serversLoad);
            }
        });
    }

    public void setAtomicCount_(int newCount){
        atomicCount.set(atomicCount.get() + newCount);
        serverDetailsController.sendTotalTasks(atomicCount.get());

    }

    public void updateNewServers() {
        for (ServerObject server : servers.values()) {
            String serverId = server.getServerId();
            if(!runningServers.containsKey(serverId)){
                Future<?> future = executorService.submit(() -> server(server));
                serverTaskMap.put(serverId, future);

                runningServers.put(serverId, null);
                aliveServers.put(
                        serverId,
                        checkingHeartBeatIntervals/makingHeartBeatIntervals - 1
                );

                serversDetails.put(
                        serverId,
                        new HashMap<>(Map.of(
                                "speed", server.getServerSpeed(),
                                "capacity",  (double)server.getQueueServer().remainingCapacity())));

                System.out.printf("Server %s started. Capacity = %s\n", serverId, server.getQueueServer().remainingCapacity());
            }
        }
        serverDetailsController.sendServerInit(serversDetails);
    }

    private void server(ServerObject server) {
        String serverId = server.getServerId();
        double serverSpeed = server.getServerSpeed();
        BlockingQueue<KeyValueObject> queueServer = server.getQueueServer();
        long waitFromCreate;

        AtomicBoolean isRunning = new AtomicBoolean(true);
        serverDetailsController.sendServerDetails(serverId, queueServer.size());
        serversLoad.put(serverId, queueServer.size());


        Future<?> heartbeatFuture = executorService.submit(() -> {
            while (isRunning.get()) {
                aliveServers.merge(serverId, 1, Integer::sum);
                try {
                    Thread.sleep(makingHeartBeatIntervals);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.printf("Heartbeat thread for server %s terminated.\n", serverId);
        });

        try {
            while (!Thread.currentThread().isInterrupted() && isRunning.get()) {
                serverDetailsController.sendServerDetails(serverId, queueServer.size());
                serversLoad.put(serverId, queueServer.size());

                if(queueServer.isEmpty()){
                    sendEmptyServer(serverId);
                }
                KeyValueObject keyValueObject = queueServer.take();

                currentWorkingTasks.put(serverId, keyValueObject);

                keyValueObject.setServerKey(serverId);
                keyValueObject.setStartOfProcessAt(System.currentTimeMillis());

                Thread.sleep((long) ((keyValueObject.getWeight() / serverSpeed) * 1000));

                keyValueObject.setEndOfProcessAt(System.currentTimeMillis());
                keyValueObject.setExecuted(true);
                currentWorkingTasks.remove(serverId);

                waitFromCreate = keyValueObject.getStartOfProcessAt() - keyValueObject.getGeneratedAt();
                atomicTotalWait.set(atomicTotalWait.get() + waitFromCreate);
                taskCompletion.merge(serverId, 1, Integer::sum);

                System.out.println("completed: "+keyValueObject.getKey()+", priority: "+keyValueObject.getPriority());

                KeyValueObjectEntity entity = new KeyValueObjectEntity();
                entity.setKey(keyValueObject.getKey());
                entity.setValue(keyValueObject.getValue());
                entity.setWeight(keyValueObject.getWeight());
                entity.setGeneratedAt(keyValueObject.getGeneratedAt());
                entity.setExecuted(keyValueObject.isExecuted());
                entity.setPriority(keyValueObject.getPriority());
                entity.setStartOfProcessAt(keyValueObject.getStartOfProcessAt());
                entity.setEndOfProcessAt(keyValueObject.getEndOfProcessAt());
                entity.setServerKey(keyValueObject.getServerKey());

                try {
                    keyValueObjectRepository.save(entity);
                } catch (Exception e) {
                    throw new CustomDatabaseException("Failed to save entity: " + entity, e);
                }


//                synchronized (sb) {
//                    sb.append(keyValueObject).append("\n");
//                }

                if (atomicCount != null) {
                    if (atomicCount.get() > 1) {
                        atomicCount.decrementAndGet();
                        System.out.println("atomic count: "+atomicCount);
                    } else {
                        System.out.println("atomic count: "+atomicCount);
                        atomicCount = new AtomicInteger();

                        String result = "Sum of wait from generate to process (" +
                                taskCompletion.values().stream().mapToInt(Integer::intValue).sum() +
                                " tasks): " + TimeUnit.MILLISECONDS.toSeconds(atomicTotalWait.get()) + " seconds.\n" +
                                "Handled by servers: " + taskCompletion + "\n";


                        taskCompletion.clear();
                        atomicTotalWait.set(0);
                        emitter.sendUpdate(result);
                        System.out.println(result);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.printf("Server %s processing interrupted.\n", serverId);
        } finally {
            isRunning.set(false);
            try {
                heartbeatFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                System.err.printf("Heartbeat task for server %s encountered an error: %s\n", serverId, e.getMessage());
            }
        }
    }

    private void checkingHeartBeat(int idealHeartBeat){
        Iterator<Map.Entry<String, Integer>> iterator = aliveServers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            String serverId = entry.getKey();
            Integer heartBeat = entry.getValue();

            if (heartBeat < idealHeartBeat) {
                runningServers.remove(serverId);
                ServerObject server = servers.remove(serverId);
                serversDetails.put(
                        serverId,
                        new HashMap<>(Map.of(
                                "speed", server.getServerSpeed(),
                                "capacity",  0.0
                        )));

                List<KeyValueObject> crashedTasksList = new ArrayList<>(server.getQueueServer());

                if (currentWorkingTasks.containsKey(server.getServerId())) {
                    crashedTasksList.add(currentWorkingTasks.get(server.getServerId()));
                }

                serverDetailsController.sendServerDetails(serverId, 0.0);
                serversLoad.put(serverId, 0);

                System.out.printf("Crashed tasks - %s: %s\n", serverId, crashedTasksList.stream().map(KeyValueObject::getKey).toList());

                sendCrashedServerTasks(crashedTasksList, serverId);

                serverDetailsController.sendServerInit(serversDetails);

                iterator.remove();
            } else {
                entry.setValue(0);
            }
        }

    }

    private void sendEmptyServer(String serverId){
        try {
            String url = "http://server1:8083/consumer-one/empty-server?serverId=" + serverId;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

//            System.out.printf("Successfully sent tasks. Response: %s\n", response.getBody());
        } catch (Exception e) {
            System.err.printf("Error sending to Server 2: %s\n", e.getMessage());
        }
    }

    public void sendCrashedServerTasks(List<KeyValueObject> crashedTasksList, String serverId) {
        try {
            String url = "http://server1:8083/consumer-one/crashed-tasks?serverId=" + serverId;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<KeyValueObject>> request = new HttpEntity<>(crashedTasksList, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

//            System.out.printf("Successfully sent tasks. Response: %s\n", response.getBody());
        } catch (Exception e) {
            System.err.printf("Error sending to Server 2: %s\n", e.getMessage());
        }
    }
}