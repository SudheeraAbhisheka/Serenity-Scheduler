package org.example.servers.service;

import com.example.AlgorithmRequestObj;
import com.example.KeyValueObject;
import com.example.ServerObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.example.servers.controller.ServerController;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ServerSimulator {
    @Getter
    @Setter
    private ConcurrentHashMap<String, ServerObject> servers;

    private ExecutorService executorService;

    public void startServerSim() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        executorService = Executors.newCachedThreadPool();

        for (ServerObject server : servers.values()) {
            executorService.submit(() -> processServerQueue(server));
        }
    }

    public void setNewServers(ConcurrentHashMap<String, ServerObject> newServers) {
        if (!executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        servers.putAll(newServers);
        for (ServerObject server : newServers.values()) {
            executorService.submit(() -> processServerQueue(server));
        }
    }

    private void processServerQueue(ServerObject server) {
        String serverId = server.getServerId();
        double serverSpeed = server.getServerSpeed();
        BlockingQueue<KeyValueObject> queueServer = server.getQueueServer();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                KeyValueObject keyValueObject = queueServer.take();
                Thread.sleep((long) ((keyValueObject.getWeight() / serverSpeed) * 1000));

                keyValueObject.setExecuted(true);
                System.out.printf("Server %s (queue size: %s) %s\n", serverId, queueServer.size(), keyValueObject);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            System.out.printf("Server %s processing interrupted.\n", serverId);
        }
    }
}