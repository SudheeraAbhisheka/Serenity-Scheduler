package org.example.servers.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/servers")
public class ServerControllerEmitter {
    // Instead of per serverId, we now keep a global list of emitters.
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // No timeout
        emitters.add(emitter);

        // Remove the emitter on completion or timeout
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    // No serverId required now; just send a message to everyone
    public void sendUpdate(String message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("update").data(message));
            } catch (IOException e) {
                emitters.remove(emitter); // If there's an error, remove the emitter
            }
        }
    }
}
