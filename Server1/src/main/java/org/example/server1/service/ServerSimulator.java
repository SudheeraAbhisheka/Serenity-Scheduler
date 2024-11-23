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

    private final List<BlockingQueue<KeyValueObject>> queues;
    private final BlockingQueue<Integer> blockingQueue = new LinkedBlockingQueue<>();

    public ServerSimulator() {
        queues = new ArrayList<>();

        BlockingQueue<KeyValueObject> queueServer1 = new LinkedBlockingQueue<>();
        BlockingQueue<KeyValueObject> queueServer2 = new LinkedBlockingQueue<>();

        queues.add(queueServer1);
        queues.add(queueServer2);

        blockingQueue.add(0);
        blockingQueue.add(1);

        new Thread(this::taskScheduler).start();

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

//    public void taskScheduler() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        KeyValueObject keyValueObject;
//        String message;
//
//        while (true) {
//            try {
//                // Check if any queue is empty
//                Queue<KeyValueObject> emptyQueue = null;
//                for (Queue<KeyValueObject> queue : queues) {
//                    if (queue.isEmpty()) {
//                        emptyQueue = queue;
//
//                        break;
//                    }else{
////                        isQuesesEmptyBQ.take();
//                    }
//                }
//
//                if (emptyQueue != null) {
//                    // Take a message only if an empty queue exists
//                    message = ConsumerOne.getMessageBlockingQueue1().take();
//                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);
//
//                    // Add the message to the empty queue
//                    emptyQueue.add(keyValueObject);
////                    System.out.println("Message added to empty queue. Queue size: " + emptyQueue.size());
//                } else {
//                    // Sleep briefly if no empty queue is available
//                    Thread.sleep(100);
//                }
//
//            } catch (InterruptedException e) {
//                throw new RuntimeException("Interrupted while retrieving message from queue", e);
//            } catch (JsonProcessingException e) {
//                System.err.println("Failed to parse message: " + e.getMessage());
//                e.printStackTrace();
//            }
//        }
//    }

    public void taskScheduler() {
        ObjectMapper objectMapper = new ObjectMapper();
        KeyValueObject keyValueObject;
        String message;

        while (true) {
            try {
                message = ConsumerOne.getMessageBlockingQueue1().take();
                keyValueObject = objectMapper.readValue(message, KeyValueObject.class);


//                while (!blockingQueue.isEmpty()) {
//                    Integer value = blockingQueue.take();
//                }
//
//                for(int item : blockingQueue){
//
//
//
//                }

                Integer queue_number = blockingQueue.take();

                System.out.println("message added to the queue "+queue_number);

                queues.get(queue_number).add(keyValueObject);

                blockingQueue.add(queue_number);



            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while retrieving message from queue", e);
            } catch (JsonProcessingException e) {
                System.err.println("Failed to parse message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void serverSimulator1() {
        int QUEUE_NUMBER = 0;
        int SLEEP_TIME = 2000;
        BlockingQueue<KeyValueObject> queue = queues.get(QUEUE_NUMBER);
        KeyValueObject keyValueObject;

        try {
            keyValueObject = queue.take();

            blockingQueue.remove(QUEUE_NUMBER);
            Thread.sleep(SLEEP_TIME);

            if(queue.isEmpty()){
                blockingQueue.add(QUEUE_NUMBER);
            }



        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        keyValueObject.setExecuted(true);
        System.out.printf("Server 1 (queue size: %s) %s\n", queue.size(), keyValueObject);
    }

    private void serverSimulator2() {
        int QUEUE_NUMBER = 1;
        int SLEEP_TIME = 1000;
        BlockingQueue<KeyValueObject> queue = queues.get(QUEUE_NUMBER);
        KeyValueObject keyValueObject;

        try {
            keyValueObject = queue.take();

            blockingQueue.remove(QUEUE_NUMBER);
            Thread.sleep(SLEEP_TIME);

            if(queue.isEmpty()){
                blockingQueue.add(QUEUE_NUMBER);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        keyValueObject.setExecuted(true);
        System.out.printf("Server 2 (queue size: %s) %s\n", queue.size(), keyValueObject);
    }
}

