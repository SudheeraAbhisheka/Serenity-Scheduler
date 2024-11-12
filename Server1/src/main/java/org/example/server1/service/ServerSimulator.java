package org.example.server1.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server1.component.ConsumerOne;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class ServerSimulator {

    private final List<Queue<KeyValueObject>> queues;
    private int currentIndex;

    public ServerSimulator() {
        queues = new ArrayList<>();

        Queue<KeyValueObject> queueServer1 = new ConcurrentLinkedQueue<>();
        Queue<KeyValueObject> queueServer2 = new ConcurrentLinkedQueue<>();

        queues.add(queueServer1);
        queues.add(queueServer2);

        // Start the taskScheduler in a new thread
        new Thread(this::taskScheduler).start();
    }

    public void taskScheduler() {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyValueObject keyValueObject;
        String message;
        int load;

        while (true) {
            try {
                message = ConsumerOne.getMessageBlockingQueue1().take();
                keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                load = keyValueObject.getLoad();
                queues.get(currentIndex).add(keyValueObject);
                System.out.println("message added");
                currentIndex = (currentIndex + 1) % queues.size();

            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while retrieving message from queue", e);

            } catch (JsonProcessingException e) {
                System.err.println("Failed to parse message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @Scheduled(fixedDelay = 2000)
    private void serverSimulator1() {
        Queue<KeyValueObject> queue = queues.get(0);
        KeyValueObject keyValueObject;

        if (!queue.isEmpty()) {
            keyValueObject = queue.poll();

            keyValueObject.setExecuted(true);
            System.out.printf("Server 1 (%s) %s\n",queue.size(), keyValueObject);



        }
    }

    @Scheduled(fixedDelay = 1000)
    private void serverSimulator2() {
        Queue<KeyValueObject> queue = queues.get(1);
        KeyValueObject keyValueObject;

        if (!queue.isEmpty()) {
            keyValueObject = queue.poll();

            keyValueObject.setExecuted(true);
            System.out.printf("Server 2 (%s) %s\n",queue.size(), keyValueObject);



        }
    }
}
