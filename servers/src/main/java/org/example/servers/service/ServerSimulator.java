package org.example.servers.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.example.servers.controller.ServerController;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ServerSimulator {
    @Getter
    private final BlockingQueue<KeyValueObject> queueServer1 = new LinkedBlockingQueue<>(1);
    @Getter
    private final BlockingQueue<KeyValueObject> queueServer2 = new LinkedBlockingQueue<>(1);

    public ServerSimulator() {
        new Thread(() -> {
            while (true) {
                serverSimulator1();

            }
        }).start();

        new Thread(() -> {
            while (true) {
                serverSimulator2();

            }
        }).start();
    }

    private void serverSimulator1() {
        int SERVER_SPEED = 1;
        KeyValueObject keyValueObject;

        try {
            keyValueObject = queueServer1.take();
            Thread.sleep((long) keyValueObject.getWeight() * 100 / SERVER_SPEED);
            keyValueObject.setExecuted(true);
            System.out.printf("Server 1 (queue size: %s) %s\n", queueServer1.size(), keyValueObject);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void serverSimulator2() {
        int SERVER_SPEED = 200;
        KeyValueObject keyValueObject;

        try {
            keyValueObject = queueServer2.take();
            Thread.sleep((long) SERVER_SPEED *keyValueObject.getWeight());
            keyValueObject.setExecuted(true);
            System.out.printf("Server 2 (queue size: %s) %s\n", queueServer2.size(), keyValueObject);


        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

