package org.example.servers.service;

import com.example.KeyValueObject;
import com.example.ServerObject;
import lombok.Getter;
import lombok.Setter;
import org.example.servers.controller.ServerControllerEmitter;
import org.example.servers.controller.ServerDetailsController;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Setter
    private AtomicInteger atomicCount = null;
    private final StringBuffer sb = new StringBuffer();
    ServerControllerEmitter emitter;
    private final AtomicLong atomicTotalWait = new AtomicLong(0);
    @Getter
    private final ConcurrentHashMap<String, Integer> handledByServer = new ConcurrentHashMap<>();
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


    public ServerSimulator(ServerControllerEmitter emitter, RestTemplate restTemplate, ServerDetailsController serverDetailsController) {
        this.emitter = emitter;
        this.restTemplate = restTemplate;
        this.serverDetailsController = serverDetailsController;

        new Thread(() -> {
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
        }).start();

    }

    public void updateServerSim() {
        for (ServerObject server : servers.values()) {
            if(!runningServers.containsKey(server.getServerId())){
                Future<?> future = executorService.submit(() -> processServerQueue(server));
                serverTaskMap.put(server.getServerId(), future);

                runningServers.put(server.getServerId(), null);
                aliveServers.put(
                        server.getServerId(),
                        checkingHeartBeatIntervals/makingHeartBeatIntervals - 1
                );

                serversDetails.put(
                        server.getServerId(),
                        new HashMap<>(Map.of(
                                "speed", server.getServerSpeed(),
                                "capacity",  (double)server.getQueueServer().remainingCapacity())));

                System.out.printf("Server %s started. Capacity = %s\n", server.getServerId(), server.getQueueServer().remainingCapacity());
            }
        }
        serverDetailsController.sendServerInit(serversDetails);
    }

    private void processServerQueue(ServerObject server) {
        String serverId = server.getServerId();
        double serverSpeed = server.getServerSpeed();
        BlockingQueue<KeyValueObject> queueServer = server.getQueueServer();
        long waitFromCreate;

        AtomicBoolean isRunning = new AtomicBoolean(true);
        serverDetailsController.sendServerDetails(serverId, queueServer.size());


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
                KeyValueObject keyValueObject = queueServer.take();

                currentWorkingTasks.put(serverId, keyValueObject);

                keyValueObject.setServerKey(serverId);
                keyValueObject.setStartOfProcessAt(System.currentTimeMillis());

                Thread.sleep((long) ((keyValueObject.getWeight() / serverSpeed) * 1000));

                keyValueObject.setEndOfProcessAt(System.currentTimeMillis());
                keyValueObject.setExecuted(true);
                currentWorkingTasks.remove(serverId);
                System.out.println("completed: "+keyValueObject.getKey());

                if (atomicCount != null) {
                    if (atomicCount.get() > 1) {
                        atomicCount.decrementAndGet();
                        System.out.println("atomic count: "+atomicCount);
//                        synchronized (sb) {
//                            sb.append(keyValueObject).append("\n");
//                        }

                        waitFromCreate = keyValueObject.getStartOfProcessAt() - keyValueObject.getGeneratedAt();
                        atomicTotalWait.set(atomicTotalWait.get() + waitFromCreate);

                        handledByServer.merge(serverId, 1, Integer::sum);
                    } else {
                        System.out.println("atomic count: "+atomicCount);
                        atomicCount = null;

                        waitFromCreate = keyValueObject.getStartOfProcessAt() - keyValueObject.getGeneratedAt();
                        atomicTotalWait.set(atomicTotalWait.get() + waitFromCreate);

                        handledByServer.merge(serverId, 1, Integer::sum);

                        synchronized (sb) {
//                            sb.append(keyValueObject).append("\n");
                            sb.append("Sum of wait from generate to process: ").append(atomicTotalWait).append(" milliseconds.\n");
                            sb.append("Handled by servers: ").append(handledByServer).append("\n");
                        }

                        generateReport();
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


    private void generateReport(){
        String s = sb.toString();
        sb.setLength(0);
        atomicTotalWait.set(0);
        handledByServer.clear();
        emitter.sendUpdate(s);
        System.out.println(s);
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

                System.out.printf("Crashed tasks - %s: %s\n", serverId, crashedTasksList.stream().map(KeyValueObject::getKey).toList());

                sendCrashedServerTasks(crashedTasksList, serverId);

                iterator.remove();
            } else {
                entry.setValue(0);
            }
        }

        serverDetailsController.sendServerInit(serversDetails);

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