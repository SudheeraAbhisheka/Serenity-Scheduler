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
    private RestTemplate restTemplate;

    public SchedulingAlgorithms(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setSchedulingAlgorithm(String algorithm){
        switch(algorithm){
            case "complete-and-then-fetch": completeAndThenFetchModel();
            case "priority-based-scheduling": priorityBasedScheduling();

        }

    }

    public void completeAndThenFetchModel() {
         new Thread(() -> {
            while (true) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject;
                String message;

                try {
                    message = ConsumerOne.getBlockingQueueCompleteF().take();
                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                    sendToServer1(keyValueObject);

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
                    message = ConsumerOne.getBlockingQueueCompleteF().take();
                    keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                    sendToServer2(keyValueObject);

                } catch (InterruptedException | JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public void priorityBasedScheduling(){
        Queue<ArrivedTimeObject> queuePriority1 = new ConcurrentLinkedQueue<>();
        Queue<ArrivedTimeObject> queuePriority2 = new ConcurrentLinkedQueue<>();
        Queue<ArrivedTimeObject> queuePriority3 = new ConcurrentLinkedQueue<>();
        BlockingQueue<Integer> bq_indicator = new LinkedBlockingQueue<>(1);

        new Thread(() -> {
            while (true) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject;
                String message;

                try {
                    message = ConsumerOne.getBlockingQueuePriorityS().take();
                    bq_indicator.offer(0);
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
                    System.out.println(age);

                    if(age/1000 > 10 ){
                        oldObjects.add(new OldObject(age, queuePriority1.poll().getKeyValueObject()));
                    }

                }
                if(!queuePriority2.isEmpty()){
                    long age = System.currentTimeMillis() - queuePriority2.peek().getArrivedTime();
                    System.out.println(age);

                    if(age/1000 > 15 ){
                        oldObjects.add(new OldObject(age, queuePriority2.poll().getKeyValueObject()));
                    }

                }
                if(!queuePriority3.isEmpty()){
                    long age = System.currentTimeMillis() - queuePriority3.peek().getArrivedTime();
                    System.out.println(age);

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
                        System.out.println("paused");
                        bq_indicator.poll();

                        try {
                            bq_indicator.take();

                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
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

    private void sendToServer2(KeyValueObject keyValueObject) {
        try {
            String url = "http://servers:8084/api/server2";
            ResponseEntity<String> response = restTemplate.postForEntity(url, keyValueObject, String.class);

        } catch (Exception e) {
            System.err.printf("Error sending to Server 2: %s\n", e.getMessage());
        }
    }
}
