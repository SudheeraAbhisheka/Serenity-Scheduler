package org.example.servers.controller;

import com.example.KeyValueObject;
import com.example.ServerObject;
import org.example.servers.service.ServerSimulator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
@RequestMapping("/api")
public class ServerController {
    private final ServerSimulator serverSimulator;
    private ConcurrentHashMap<String, ServerObject> servers;

    public ServerController() {
        this.serverSimulator = new ServerSimulator();
    }

    @PostMapping("/set-servers")
    public ResponseEntity<String> setServers(@RequestBody LinkedHashMap<String, Double> linkedHashMap) {
        ConcurrentHashMap<String, ServerObject> servers = new ConcurrentHashMap<>();

        for(Map.Entry<String, Double> entry : linkedHashMap.entrySet()) {
            servers.put(entry.getKey(), new ServerObject(entry.getKey(), new LinkedBlockingQueue<>(1), entry.getValue()));
            servers.put(entry.getKey(), new ServerObject(entry.getKey(), new LinkedBlockingQueue<>(1), entry.getValue()));
        }
        serverSimulator.setServers(
                servers
        );

        this.servers = serverSimulator.getServers();
        serverSimulator.StartServerSim();

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/server")
    public ResponseEntity<String> handleServer1(@RequestParam String serverId, @RequestBody KeyValueObject keyValueObject) throws InterruptedException {
//        for(String serverID2 : servers.keySet()) {
//            if(serverID.equals(serverID2)){
//                servers.get(serverID).getQueueServer().put(keyValueObject);
//
//            }
//        }
//
//        if(servers.containsKey(serverID)) {
            servers.get(serverId).getQueueServer().put(keyValueObject);
//        }

//        if(serverId.equals("1")){
//            servers.get(serverId).getQueueServer().put(keyValueObject);
//
//        }
//        if(serverId.equals("2")){
//            servers.get(serverId).getQueueServer().put(keyValueObject);
//        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/server2")
    public ResponseEntity<String> handleServer2(@RequestBody KeyValueObject keyValueObject) {
        try {
            servers.get("1").getQueueServer().put(keyValueObject);

            return ResponseEntity.status(HttpStatus.OK).body("Data processed by Server 2");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing data at Server 2: " + e.getMessage());
        }
    }
//
//    @GetMapping("/servers")
//    public ConcurrentHashMap<String, ServerObject> getServers() {
//        return new ConcurrentHashMap<>(){{
//            put("1", new ServerObject("1", new LinkedBlockingQueue<>(1), 20));
//            put("2", new ServerObject("2", new LinkedBlockingQueue<>(1), 0.5));
//
//        }};
//    }
}