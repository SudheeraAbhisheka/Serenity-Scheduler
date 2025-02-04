package org.example.servers.controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class WebSocketController {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendServerInit(Map<String, Map<String, Double>> serversDetails) {
        messagingTemplate.convertAndSend("/topic/serverInit", serversDetails);
    }

    public void sendServerDetails(Map<String, Integer> serversLoad) {
        messagingTemplate.convertAndSend("/topic/serverDetails", serversLoad);
    }

    public void sendTotalTasks(Integer totalTasks) {
        messagingTemplate.convertAndSend("/topic/taskCompletionTotalTasks", totalTasks);
    }

    public void sendTaskCompletion(Map<String, Integer> taskCompletion) {
        messagingTemplate.convertAndSend("/topic/taskCompletion", taskCompletion);
    }

    public void sendResult(String result) {
        Map<String, Object> data = new HashMap<>();
        data.put("text", result);
        messagingTemplate.convertAndSend("/topic/servers", data);
    }
}
