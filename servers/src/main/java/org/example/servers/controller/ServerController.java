package org.example.servers.controller;

import com.example.KeyValueObject;
import com.example.ServerObject;
import org.example.servers.service.ServerSimulator;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
@RequestMapping("/api")
public class ServerController {
    private final ServerSimulator serverSimulator;
    private final RestTemplate restTemplate;


    public ServerController(RestTemplate restTemplate, ServerSimulator serverSimulator) {
        this.restTemplate = restTemplate;
        this.serverSimulator = serverSimulator;
    }

    @PostMapping("/set-servers")
    public ResponseEntity<String> setServers(@RequestBody LinkedHashMap<String, Double> initialServers) {
        ConcurrentHashMap<String, ServerObject> servers = new ConcurrentHashMap<>();

        for(Map.Entry<String, Double> entry : initialServers.entrySet()) {
            servers.put(entry.getKey(), new ServerObject(entry.getKey(), new LinkedBlockingQueue<>(1), entry.getValue()));
        }

        serverSimulator.setServers(servers);
        serverSimulator.startServerSim();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/set-new-servers")
    public ResponseEntity<String> setNewServers(@RequestBody LinkedHashMap<String, Double> newServersL) {
        ConcurrentHashMap<String, ServerObject> newServersC = new ConcurrentHashMap<>();

        for(Map.Entry<String, Double> entry : newServersL.entrySet()) {
            newServersC.put(entry.getKey(), new ServerObject(entry.getKey(), new LinkedBlockingQueue<>(1), entry.getValue()));
        }

        serverSimulator.setNewServers(newServersC);
        notifyNewServers(newServersL);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/server")
    public ResponseEntity<String> handleServer1(@RequestParam String serverId, @RequestBody KeyValueObject keyValueObject) throws InterruptedException {
        serverSimulator.getServers().get(serverId).getQueueServer().put(keyValueObject);

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
        LinkedHashMap<String, Double> linkedHashMap = new LinkedHashMap<>();

        for(ServerObject serverObject : serverSimulator.getServers().values()) {
            linkedHashMap.put(serverObject.getServerId(), serverObject.getServerSpeed());
        }
        return linkedHashMap;
    }

    public void notifyNewServers(LinkedHashMap<String, Double> newServers){
        String url = "http://server1:8083/consumer-one/set-new-servers";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        boolean setSuccess = false;

        HttpEntity<LinkedHashMap<String, Double>> request = new HttpEntity<>(newServers, headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("New servers set successfully.");
                setSuccess = true;
            } else {
                System.out.println("Failed to set algorithm. Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while setting algorithm: " + e.getMessage());
            e.printStackTrace();
        }
    }
}