package org.example.server1.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.component.RabbitMQ_consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class SchedulingAlgorithms {
    private final RestTemplate restTemplate;
    private boolean waitingThreads = false;
//    private ConcurrentHashMap<String, ServerObject> servers;
    private LinkedHashMap<String, Double> servers;

    @Autowired
    public SchedulingAlgorithms(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setSchedulingAlgorithm(String algorithm){
        servers= new LinkedHashMap<>(){{
            put("1", 10.0);
            put("2", 20.0);
        }};

        setServers();

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
//        for(ServerObject serverObject : fetchServers().values()){
        for(String serverId : servers.keySet()){
            new Thread(() -> {
                ObjectMapper objectMapper = new ObjectMapper();

                while (true) {
                    KeyValueObject keyValueObject;
                    String message;

                    try {
//                        message = RabbitMQ_consumer.getBlockingQueueCompleteF().take();
                        message = Kafka_consumer.getBlockingQueueCompleteF().take();
                        keyValueObject = objectMapper.readValue(message, KeyValueObject.class);

                        try {
                            String url = "http://servers:8084/api/server?serverId=" + serverId;
//                            String url = "http://servers:8084/api/server" + serverId;
                            ResponseEntity<String> response = restTemplate.postForEntity(url, keyValueObject, String.class);

                        } catch (Exception e) {
                            System.err.printf("Error sending to Server %s: %s\n", serverId, e.getMessage());
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

        new Thread(() -> {
            while (true) {
                ObjectMapper objectMapper = new ObjectMapper();
                KeyValueObject keyValueObject;
                String message;

                try {

//                    message = RabbitMQ_consumer.getBlockingQueuePriorityS().take();
                    message = Kafka_consumer.getBlockingQueuePriorityS().take();

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
                    sendToServer2(k);
                }
                else{
                    if(!queuePriority1.isEmpty()){
                        sendToServer2(queuePriority1.poll().getKeyValueObject());

                    }
                    else if(!queuePriority2.isEmpty()){
                        sendToServer2(queuePriority2.poll().getKeyValueObject());

                    }
                    else if(!queuePriority3.isEmpty()){
                        sendToServer2(queuePriority3.poll().getKeyValueObject());

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

    private void sendToServer2(KeyValueObject keyValueObject) {
        try {
            String url = "http://servers:8084/api/server2";
            ResponseEntity<String> response = restTemplate.postForEntity(url, keyValueObject, String.class);

        } catch (Exception e) {
            System.err.printf("Error sending to Server 2: %s\n", e.getMessage());
        }
    }

    private void setServers() {
        try {
            String url = "http://servers:8084/api/set-servers";
            ResponseEntity<String> response = restTemplate.postForEntity(url, servers, String.class);

        } catch (Exception e) {
            System.err.printf("Error sending to Server 2: %s\n", e.getMessage());
        }
    }

//    @Autowired
//    private ObjectMapper objectMapper;
//
//    public ConcurrentHashMap<String, ServerObject> fetchServers() {
//        ConcurrentHashMap<String, ServerObject> servers = new ConcurrentHashMap<>();
//
//        Map<String, LinkedHashMap> rawResponse = restTemplate.getForObject(
//                "http://servers:8084/api/servers",
//                Map.class
//        );
//
//        rawResponse.forEach((key, value) -> {
//            try {
//                String id = String.valueOf(key);
//                System.out.println("sever id: "+id);
//                ServerObject serverObject = objectMapper.convertValue(value, new TypeReference<>() {
//                });
//                System.out.println("server object: "+serverObject);
//
//                servers.put(id, serverObject);
//            } catch (IllegalArgumentException e) {
//                // Log and handle the error if deserialization fails for any entry
//                System.out.printf("Error deserializing ServerObject for key %s {%s}: {%s}\n", key, e.getMessage(), e);
//            }
//        });
//
//        return servers;
//    }

}
