package org.example.servers.service;

import com.example.KeyValueObject;
import com.example.ServerObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.example.servers.controller.ServerController;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerSimulator {
    @Getter
    @Setter
    private ConcurrentHashMap<String, ServerObject> servers;

    public ServerSimulator() {}

    public void StartServerSim() {
        for(ServerObject server : servers.values()) {
            new Thread(() -> {
                String serverId = server.getServerId();
                double serverSpeed = server.getServerSpeed();
                BlockingQueue<KeyValueObject> queueServer = server.getQueueServer();

                while (true) {
                    KeyValueObject keyValueObject;

                    try {
                        keyValueObject = queueServer.take();
                        Thread.sleep((long) ((keyValueObject.getWeight() / serverSpeed) * 1000));

                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    keyValueObject.setExecuted(true);
                    System.out.printf("Server %s (queue size: %s) %s\n", serverId, queueServer.size(), keyValueObject);

                }
            }).start();

        }

    }
}