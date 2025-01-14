package org.example.servers.service;

import com.example.KeyValueObject;
import com.example.ServerObject;
import lombok.Getter;
import lombok.Setter;
import org.example.servers.controller.ServerControllerEmitter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final ConcurrentHashMap<String, Integer> handledByServer = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<String, Future<?>> serverTaskMap = new ConcurrentHashMap<>();

    public ServerSimulator(ServerControllerEmitter emitter) {
        this.emitter = emitter;
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

        // Shared flag to control thread termination
        AtomicBoolean isRunning = new AtomicBoolean(true);

        Thread heartbeatThread = new Thread(() -> {
            while (isRunning.get()) {
                aliveServers.merge(serverId, 1, Integer::sum);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Respect the interrupt signal
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

                keyValueObject.setServerKey(serverId);
                keyValueObject.setStartOfProcessAt(System.currentTimeMillis());
                Thread.sleep((long) ((keyValueObject.getWeight() / serverSpeed) * 1000));
                keyValueObject.setEndOfProcessAt(System.currentTimeMillis());
                keyValueObject.setExecuted(true);

                if (atomicCount != null) {
                    if (atomicCount.get() > 1) {
                        atomicCount.decrementAndGet();
                        synchronized (sb) {
                            sb.append(keyValueObject).append("\n");
                        }

                        waitFromCreate = keyValueObject.getStartOfProcessAt() - keyValueObject.getGeneratedAt();
                        atomicTotalWait.set(atomicTotalWait.get() + waitFromCreate);

                        handledByServer.merge(serverId, 1, Integer::sum);
                    } else {
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
            isRunning.set(false); // Stop the heartbeat thread
            try {
                heartbeatThread.join(); // Ensure the thread terminates
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
        System.out.println(s);
    }

    private void checkingHeartBeat(){
        System.out.println("alive servers - before: " + aliveServers);
        for (Map.Entry<String, Integer> entry : aliveServers.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();

            if(value < 1){
                System.out.println("Server " + key + " has low heartbeat");
//                        crashedServer(servers.get(key));
//                        servers.remove(key);
//                        runningServers.remove(key);
            }

            entry.setValue(0);
        }
        System.out.println("alive servers - after: " + aliveServers);
    }

    private void crashedServer(ServerObject server){
        BlockingQueue<KeyValueObject> queueServer = server.getQueueServer();

        System.out.println(queueServer);

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
}