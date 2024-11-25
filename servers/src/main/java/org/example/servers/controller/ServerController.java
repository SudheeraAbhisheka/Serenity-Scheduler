package org.example.servers.controller;

import com.example.KeyValueObject;
import org.example.servers.service.ServerSimulator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
@RequestMapping("/api")
public class ServerController {
    private ServerSimulator serverSimulator;

    public ServerController(ServerSimulator serverSimulator) {
        this.serverSimulator = serverSimulator;
    }

    @PostMapping("/server1")
    public ResponseEntity<String> handleServer1(@RequestBody KeyValueObject keyValueObject) {
        try {
            serverSimulator.queueServer1.put(keyValueObject);

            return ResponseEntity.status(HttpStatus.OK).body("Data processed by Server 1");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing data at Server 1: " + e.getMessage());
        }
    }

    @PostMapping("/server2")
    public ResponseEntity<String> handleServer2(@RequestBody KeyValueObject keyValueObject) {
        try {
            serverSimulator.queueServer2.put(keyValueObject);

            return ResponseEntity.status(HttpStatus.OK).body("Data processed by Server 2");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing data at Server 2: " + e.getMessage());
        }
    }
}
