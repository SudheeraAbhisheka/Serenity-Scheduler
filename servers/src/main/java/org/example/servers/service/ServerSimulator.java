package org.example.servers.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.servers.controller.ServerController;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ServerSimulator {
    public final BlockingQueue<KeyValueObject> queueServer1 = new LinkedBlockingQueue<>(1);
    public final BlockingQueue<KeyValueObject> queueServer2 = new LinkedBlockingQueue<>(1);

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
        int SLEEP_TIME = 2000;
        KeyValueObject keyValueObject;

        try {
            keyValueObject = queueServer1.take();
            keyValueObject.setExecuted(true);
            Thread.sleep(SLEEP_TIME);
            System.out.printf("*Server 1 (queue size: %s) %s\n", queueServer1.size(), keyValueObject);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void serverSimulator2() {
        int SLEEP_TIME = 1000;
        KeyValueObject keyValueObject;

        try {
            keyValueObject = queueServer2.take();
            keyValueObject.setExecuted(true);
            Thread.sleep(SLEEP_TIME);
            System.out.printf("**Server 2 (queue size: %s) %s\n", queueServer2.size(), keyValueObject);


        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

