package org.example.servers.controller;

import com.example.KeyValueObject;
import com.example.ServerObject;
import com.example.SpeedAndCapObj;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.servers.service.ServerSimulator;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
public class ServerController {
    private final ServerSimulator serverSimulator;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int nameOfServer = 1;


    public ServerController(RestTemplate restTemplate, ServerSimulator serverSimulator) {
        this.restTemplate = restTemplate;
        this.serverSimulator = serverSimulator;
    }

    @PostMapping("/set-heart-beat-intervals")
    public ResponseEntity<String> setHeartBeatIntervals(@RequestBody Map<String, Integer> heartBeatIntervals) {
        int checkingIntervals = heartBeatIntervals.get("checking");
        int makingIntervals = heartBeatIntervals.get("making");

        if (serverSimulator.getServers() != null || checkingIntervals > makingIntervals) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        else{
            serverSimulator.setCheckingHeartBeatIntervals(checkingIntervals);
            serverSimulator.setMakingHeartBeatIntervals(makingIntervals);

            return new ResponseEntity<>(HttpStatus.OK);
        }
    }

    @PostMapping("/set-server-many")
    public ResponseEntity<String> setServersDefault(@RequestParam int noOfQueues, @RequestBody SpeedAndCapObj speedAndCapObj) {
        if (serverSimulator.getServers() == null) {
            serverSimulator.setServers(new ConcurrentHashMap<>());
        }

        for (int i = 0; i < noOfQueues; i++) {
            serverSimulator.getServers().put(
                    Integer.toString(nameOfServer),
                    new ServerObject(Integer.toString(nameOfServer), new LinkedBlockingQueue<>(speedAndCapObj.getCap()), speedAndCapObj.getSpeed())
            );
            nameOfServer++;
        }

        serverSimulator.updateServerSim();
        notifyNewServers();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/set-server-one")
    public ResponseEntity<String> setServersOneByOne(@RequestBody SpeedAndCapObj speedAndCapObj) {
        if (serverSimulator.getServers() == null) {
            serverSimulator.setServers(new ConcurrentHashMap<>());
        }

        serverSimulator.getServers().put(
                Integer.toString(nameOfServer),
                new ServerObject(Integer.toString(nameOfServer), new LinkedBlockingQueue<>(speedAndCapObj.getCap()), speedAndCapObj.getSpeed()));

        serverSimulator.updateServerSim();
        notifyNewServers();

        nameOfServer++;

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/assigning-to-servers")
    public ResponseEntity<String> handleServer1(@RequestParam String serverId, @RequestBody KeyValueObject keyValueObject) throws InterruptedException {
        ServerObject server = serverSimulator.getServers().get(serverId);

        if (server == null) {
            System.out.printf("Null server %s,:%s\n", serverId, keyValueObject.getKey());
        }
        else{
            server.getQueueServer().put(keyValueObject);
        }


        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/wlb-algorithm")
    public ResponseEntity<String> weightLoadBalancing(@RequestBody Map<String, String> taskServersMap) throws JsonProcessingException {
        for (Map.Entry<String, String> entry : taskServersMap.entrySet()) {
            KeyValueObject keyValueObject = objectMapper.readValue(entry.getKey(), KeyValueObject.class);
            String serverId = entry.getValue();
            serverSimulator.getServers().get(serverId).getQueueServer().add(keyValueObject);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PostMapping("/server2")
    public ResponseEntity<String> handleServer2(@RequestBody KeyValueObject keyValueObject) {
        try {
            serverSimulator.getServers().get("1").getQueueServer().put(keyValueObject);

            return ResponseEntity.status(HttpStatus.OK).body("Data processed by Server 2");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing data at Server 2: " + e.getMessage());
        }
    }

    @GetMapping("/get-servers")
    public LinkedHashMap<String, Double> getServers() {
        LinkedHashMap<String, Double> serversSpeeds = new LinkedHashMap<>();

        if(serverSimulator.getServers() != null) {
            for(ServerObject serverObject : serverSimulator.getServers().values()) {
                serversSpeeds.put(serverObject.getServerId(), serverObject.getServerSpeed());
            }
        }

        System.out.println("number of servers: "+serversSpeeds.size());

        return serversSpeeds;
    }

    @GetMapping("/get-remaining-caps")
    public LinkedHashMap<String, Integer> getRemainingCaps() {
        LinkedHashMap<String, Integer> remainingCapacities = new LinkedHashMap<>();

        if(serverSimulator.getServers() != null) {
            for(ServerObject serverObject : serverSimulator.getServers().values()) {
                remainingCapacities.put(
                        serverObject.getServerId(),
                        serverObject.getQueueServer().remainingCapacity()
                );
            }
        }

        return remainingCapacities;
    }

    @GetMapping("/get-server-loads")
    public LinkedHashMap<String, Double> getServerLoads() {
        LinkedHashMap<String, Double> serverLoads = new LinkedHashMap<>();

        if(serverSimulator.getServers() != null) {
            for(ServerObject serverObject : serverSimulator.getServers().values()) {
                serverLoads.put(
                        serverObject.getServerId(),
                        getCurrentLoad(serverObject.getQueueServer(), serverObject.getServerSpeed())
                );
            }
        }

        return serverLoads;
    }

    private double getCurrentLoad(LinkedBlockingQueue<KeyValueObject> keyValueObjects, double serverSpeed){
        double remainingTime = 0.0;

        for(KeyValueObject keyValueObject : keyValueObjects) {
            remainingTime += keyValueObject.getValue() * serverSpeed;
        }

        return remainingTime;
    }

    public void notifyNewServers() {
        String url = "http://server1:8083/consumer-one/notify-new-servers";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Notification sent successfully.");
            } else {
                System.out.println("Failed to notify. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while notifying: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PostMapping("/generate-report")
    public void generateReport(@RequestBody int count) {
        serverSimulator.setAtomicCount(new AtomicInteger(count));
        serverSimulator.getHandledByServer().clear();
    }

    @PostMapping("/crash-server")
    public boolean crashAServer(@RequestBody Integer serverId) {
        Future<?> future = serverSimulator.getServerTaskMap().get(serverId.toString());
        boolean successful = false;

        if (future == null) {
            System.out.println(future);
        }
        else{
            successful = future.cancel(true);
            if(successful) {
                serverSimulator.getServerTaskMap().remove(serverId.toString());
            }
            else{
                System.out.println("Failed to crash server " + serverId);
            }
        }

        return successful;
    }
}