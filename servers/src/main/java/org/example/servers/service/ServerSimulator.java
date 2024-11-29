package org.example.servers.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.example.servers.controller.ServerController;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ServerSimulator {
    @Getter
    private final BlockingQueue<KeyValueObject> queueServer1 = new LinkedBlockingQueue<>(1);
    @Getter
    private final BlockingQueue<KeyValueObject> queueServer2 = new LinkedBlockingQueue<>(1);
    @Getter
    private final ArrayList<ServerObject> servers = new ArrayList<>();

    public ServerSimulator() {
        servers.add(
                new ServerObject(0, queueServer1, 1)
        );
        servers.add(
                new ServerObject(1, queueServer2, 2)
        );

        for(ServerObject server : servers) {
            new Thread(() -> {
                int serverId = server.getServerId();
                int serverSpeed = server.getServerSpeed();
                BlockingQueue<KeyValueObject> queueServer = server.getQueueServer();

                while (true) {
                    KeyValueObject keyValueObject;

                    try {
                        keyValueObject = queueServer.take();
                        Thread.sleep((long) keyValueObject.getWeight() * 100 / serverSpeed);

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

