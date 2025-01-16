package org.example.servers.service;

import com.example.KeyValueObject;
import com.example.ServerObject;
import lombok.Getter;
import lombok.Setter;
import org.example.servers.controller.ServerControllerEmitter;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final LinkedHashMap<String, Boolean> runningServers;
    private final ConcurrentHashMap<String, Integer> aliveServers;
    private final ExecutorService executorService;
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

    public ServerSimulator(ServerControllerEmitter emitter, RestTemplate restTemplate) {
        this.emitter = emitter;
        this.restTemplate = restTemplate;
        executorService = Executors.newCachedThreadPool();
        runningServers = new LinkedHashMap<>();
        aliveServers = new ConcurrentHashMap<>();

        new Thread(() -> {
            while(true){
                checkingHeartBeat();
                try {
                    Thread.sleep(3000);
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
                aliveServers.put(server.getServerId(), 0);

                System.out.printf("Server %s started. Capacity = %s\n", server.getServerId(), server.getQueueServer().remainingCapacity());
            }
        }
    }

    private void processServerQueue(ServerObject server) {
        String serverId = server.getServerId();
        double serverSpeed = server.getServerSpeed();
        BlockingQueue<KeyValueObject> queueServer = server.getQueueServer();
        long waitFromCreate;

        AtomicBoolean isRunning = new AtomicBoolean(true);

        Thread heartbeatThread = new Thread(() -> {
            while (isRunning.get()) {
                aliveServers.merge(serverId, 1, Integer::sum);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.printf("Heartbeat thread for server %s terminated.\n", serverId);
        });
        heartbeatThread.start();

        try {
            while (!Thread.currentThread().isInterrupted() && isRunning.get()) {
                KeyValueObject keyValueObject = queueServer.take();
                currentWorkingTasks.put(serverId, keyValueObject);

                keyValueObject.setServerKey(serverId);
                keyValueObject.setStartOfProcessAt(System.currentTimeMillis());
                Thread.sleep((long) ((keyValueObject.getWeight() / serverSpeed) * 1000));
                keyValueObject.setEndOfProcessAt(System.currentTimeMillis());
                keyValueObject.setExecuted(true);
                currentWorkingTasks.remove(serverId);
                System.out.println(keyValueObject);

                if (atomicCount != null) {
                    if (atomicCount.get() > 1) {
                        atomicCount.decrementAndGet();
                        System.out.println("atomic count: "+atomicCount);
                        synchronized (sb) {
                            sb.append(keyValueObject).append("\n");
                        }

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
                            sb.append(keyValueObject).append("\n");
                            sb.append("Sum of wait from generate to process: ").append(atomicTotalWait).append("\n");
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
                heartbeatThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    private void generateReport(){
        String s = sb.toString();
        sb.setLength(0);
        atomicTotalWait.set(0);
        handledByServer.clear();
        emitter.sendUpdate(s);
//        System.out.println(s);
    }

    private void checkingHeartBeat(){
        Iterator<Map.Entry<String, Integer>> iterator = aliveServers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            String serverId = entry.getKey();
            Integer count = entry.getValue();

            if (count < 1) {
                runningServers.remove(serverId);
//                crashedServer(servers.remove(serverId));
                ServerObject server = servers.remove(serverId);

                List<KeyValueObject> crashedTasksList = new ArrayList<>(server.getQueueServer());
                crashedTasksList.add(
                        currentWorkingTasks.get(server.getServerId())
                );
                sendCrashedServerTasks(crashedTasksList, serverId);

//                Queue<KeyValueObject> crashedTasks = server.getQueueServer();
//                crashedTasks.add(
//                        currentWorkingTasks.get(server.getServerId())
//                );
//                sendCrashedServerTasks(crashedTasks);

                iterator.remove();
            } else {
                entry.setValue(0);
            }
        }

    }

    private void crashedServer(ServerObject server){
        BlockingQueue<KeyValueObject> queueServer = server.getQueueServer();
        KeyValueObject unfinishedTask = currentWorkingTasks.get(server.getServerId());

        System.out.println("queue server: "+queueServer);

//        while(!queueServer.isEmpty()){
//            for(ServerObject serverObject : servers.values()){
//                if(queueServer.isEmpty()){
//                    return;
//                }
//                else{
//                    if(serverObject.getQueueServer().remainingCapacity() > 0){
//                        serverObject.getQueueServer().add(queueServer.remove());
//                    }
//                }
//            }
//        }
    }

    public void sendCrashedServerTasks2(Queue<KeyValueObject> queueServer) {
        try {
            String url = "http://servers:8084/api/server2";
            restTemplate.postForEntity(url, queueServer, Queue.class);
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

            System.out.printf("Successfully sent tasks. Response: %s\n", response.getBody());
        } catch (Exception e) {
            System.err.printf("Error sending to Server 2: %s\n", e.getMessage());
        }
    }
}