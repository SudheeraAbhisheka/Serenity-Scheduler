package org.example.server1.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server1.component.ConsumerOne;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class ServerSimulator {
    private final BlockingQueue<KeyValueObject> queueServer1 = new LinkedBlockingQueue<>(1);
    private final BlockingQueue<KeyValueObject> queueServer2 = new LinkedBlockingQueue<>(1);

    public ServerSimulator() {
        new Thread(() -> {
            while (true) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject;
                String message;

                try {
                    message = ConsumerOne.getMessageBlockingQueue1().take();
                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                    queueServer1.put(keyValueObject);

                } catch (InterruptedException | JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

            }
        }).start();

        new Thread(() -> {
            while (true) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject;
                String message;

                try {
                    message = ConsumerOne.getMessageBlockingQueue1().take();
                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);
                    queueServer2.put(keyValueObject);


                } catch (InterruptedException | JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

            }
        }).start();

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

