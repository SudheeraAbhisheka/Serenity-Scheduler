package org.example.server1.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server1.component.ConsumerOne;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class SchedulingAlgorithms {
    private final RestTemplate restTemplate;
    boolean waitingThreads = false;

    public SchedulingAlgorithms(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setSchedulingAlgorithm(String algorithm){
        switch (algorithm) {
            case "complete-and-then-fetch":
                completeAndThenFetchModel();
                break;
            case "age-based-priority-scheduling":
                priorityBasedScheduling();
                break;
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }


    }

    public void completeAndThenFetchModel() {
        for(int i = 0; i < 2; i++){
            int finalI = i;
            new Thread(() -> {
                ObjectMapper objectMapper = new ObjectMapper();
                while (true) {
                    KeyValueObject keyValueObject;
                    String message;

                    try {
                        message = ConsumerOne.getBlockingQueueCompleteF().take();
                        keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                        try {
                            String url = "http://servers:8084/api/server"+ (finalI+1);
                            ResponseEntity<String> response = restTemplate.postForEntity(url, keyValueObject, String.class);

                        } catch (Exception e) {
                            System.err.printf("Error sending to Server %s: %s\n", (finalI+1), e.getMessage());
                        }

                    } catch (InterruptedException | JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }
    }

    public void priorityBasedScheduling(){
        Queue<ArrivedTimeObject> queuePriority1 = new ConcurrentLinkedQueue<>();
        Queue<ArrivedTimeObject> queuePriority2 = new ConcurrentLinkedQueue<>();
        Queue<ArrivedTimeObject> queuePriority3 = new ConcurrentLinkedQueue<>();
        final Object lock = new Object();
        BlockingQueue<Integer> bq_indicator = new LinkedBlockingQueue<>(1);

        new Thread(() -> {
            while (true) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject;
                String message;

                try {

                    message = ConsumerOne.getBlockingQueuePriorityS().take();

                    if(waitingThreads){
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                } catch (InterruptedException | JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                if(keyValueObject.getPriority() == 1){
                    queuePriority1.add(
                            new ArrivedTimeObject(System.currentTimeMillis(), keyValueObject)
                    );

                } else if(keyValueObject.getPriority() == 2){
                    queuePriority2.add(
                            new ArrivedTimeObject(System.currentTimeMillis(), keyValueObject)
                    );

                } else if(keyValueObject.getPriority() == 3){
                    queuePriority3.add(
                            new ArrivedTimeObject(System.currentTimeMillis(), keyValueObject));
                }
            }
        }).start();

        new Thread(() -> {
            List<OldObject> oldObjects = new ArrayList<>();

            while (true) {
                if(!queuePriority1.isEmpty()){
                    long age = System.currentTimeMillis() - queuePriority1.peek().getArrivedTime();

                    if(age/1000 > 10 ){
                        oldObjects.add(new OldObject(age, queuePriority1.poll().getKeyValueObject()));
                    }

                }
                if(!queuePriority2.isEmpty()){
                    long age = System.currentTimeMillis() - queuePriority2.peek().getArrivedTime();

                    if(age/1000 > 15 ){
                        oldObjects.add(new OldObject(age, queuePriority2.poll().getKeyValueObject()));
                    }

                }
                if(!queuePriority3.isEmpty()){
                    long age = System.currentTimeMillis() - queuePriority3.peek().getArrivedTime();

                    if(age/1000 > 20 ){
                        oldObjects.add(new OldObject(age, queuePriority3.poll().getKeyValueObject()));
                    }

                }

                if(!oldObjects.isEmpty()){

                    oldObjects.sort(Comparator.comparingLong(OldObject::getAge).reversed());
                    KeyValueObject k = oldObjects.remove(0).getKeyValueObject();
                    sendToServer1(k);
                }
                else{
                    if(!queuePriority1.isEmpty()){
                        sendToServer1(queuePriority1.poll().getKeyValueObject());

                    }
                    else if(!queuePriority2.isEmpty()){
                        sendToServer1(queuePriority2.poll().getKeyValueObject());

                    }
                    else if(!queuePriority3.isEmpty()){
                        sendToServer1(queuePriority3.poll().getKeyValueObject());

                    }else{

                        synchronized(lock){
                            try {
                                waitingThreads = true;
                                lock.wait();

                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } finally {
                                waitingThreads = false;
                            }
                        }
                    }
                }

            }
        }).start();
    }

    private void sendToServer1(KeyValueObject keyValueObject) {
        try {
            String url = "http://servers:8084/api/server1";
            ResponseEntity<String> response = restTemplate.postForEntity(url, keyValueObject, String.class);

        } catch (Exception e) {
            System.err.printf("Error sending to Server 1: %s\n", e.getMessage());
        }
    }
}
