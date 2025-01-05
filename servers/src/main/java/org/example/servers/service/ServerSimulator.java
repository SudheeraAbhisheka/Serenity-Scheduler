package org.example.servers.service;

import com.example.KeyValueObject;
import com.example.ServerObject;
import lombok.Getter;
import lombok.Setter;
import org.example.servers.controller.ServerControllerEmitter;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ServerSimulator {
    @Getter
    @Setter
    private ConcurrentHashMap<String, ServerObject> servers;
    private final LinkedHashMap<String, Boolean> runningServers;
    private final ServerControllerEmitter serverControllerEmitter;
    private ExecutorService executorService;

    public ServerSimulator(ServerControllerEmitter serverControllerEmitter) {
        this.serverControllerEmitter = serverControllerEmitter;
        executorService = Executors.newCachedThreadPool();
        runningServers = new LinkedHashMap<>();
    }

    public void updateServerSim() {
        for (ServerObject server : servers.values()) {
            if(!runningServers.containsKey(server.getServerId())){
                executorService.submit(() -> processServerQueue(server));
                runningServers.put(server.getServerId(), null);

                System.out.printf("Server %s started. Capacity = %s\n", server.getServerId(), server.getQueueServer().remainingCapacity());
            }
        }
    }

    public void setNewServers(ConcurrentHashMap<String, ServerObject> newServers) {
        for (ServerObject newServer : newServers.values()) {
            executorService.submit(() -> processServerQueue(newServer));
            System.out.printf("Server %s started. Capacity = %s\n", newServer.getServerId(), newServer.getQueueServer().remainingCapacity());
        }
        servers.putAll(newServers);
    }

    private void processServerQueue(ServerObject server) {
        String serverId = server.getServerId();
        double serverSpeed = server.getServerSpeed();
        BlockingQueue<KeyValueObject> queueServer = server.getQueueServer();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                KeyValueObject keyValueObject = queueServer.take();

                keyValueObject.setServerKey(serverId);
                keyValueObject.setStartOfProcessAt(System.currentTimeMillis());
                Thread.sleep((long) ((keyValueObject.getWeight() / serverSpeed) * 1000));
                keyValueObject.setEndOfProcessAt(System.currentTimeMillis());
                keyValueObject.setExecuted(true);

                serverControllerEmitter.sendUpdate(keyValueObject);

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            String interruptMessage = String.format("Server %s processing interrupted.", serverId);
//            serverControllerEmitter.sendUpdate(interruptMessage);
            System.out.println(interruptMessage);
        }
    }
}