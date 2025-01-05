package org.example.servers.controller;

import com.example.KeyValueObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    public void sendUpdate(KeyValueObject keyValueObject) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Create a map or a dedicated DTO to hold the data
        Map<String, Object> data = new HashMap<>();
        data.put("key", keyValueObject.getKey());
        data.put("value", keyValueObject.getValue());
        data.put("weight", keyValueObject.getWeight());
        data.put("generatedAt", keyValueObject.getGeneratedAt());
        data.put("executed", keyValueObject.isExecuted());
        data.put("priority", keyValueObject.getPriority());
        data.put("startOfProcessAt", keyValueObject.getStartOfProcessAt());
        data.put("endOfProcessAt", keyValueObject.getEndOfProcessAt());
        data.put("serverKey", keyValueObject.getServerKey());

        try {
            String jsonMessage = objectMapper.writeValueAsString(data);

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("update").data(jsonMessage));
                } catch (IOException e) {
                    emitters.remove(emitter); // If there's an error, remove the emitter
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle JSON serialization errors
        }
    }
}
