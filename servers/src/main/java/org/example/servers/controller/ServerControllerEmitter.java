package org.example.servers.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/servers")
public class ServerControllerEmitter {
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/{serverId}/subscribe")
    public SseEmitter subscribe(@PathVariable String serverId) {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        emitters.put(serverId, emitter);

        emitter.onCompletion(() -> emitters.remove(serverId));
        emitter.onTimeout(() -> emitters.remove(serverId));

        return emitter;
    }


    public void sendUpdate(String serverId, String message) {
        SseEmitter emitter = emitters.get(serverId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("update").data(message));
            } catch (IOException e) {
                emitters.remove(serverId);
            }
        }
    }

}
