package org.example.servers.controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class ServerDetailsController {
    private final SimpMessagingTemplate messagingTemplate;

    public ServerDetailsController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendServerInit(Map<String, Map<String, Double>> serversDetails) {
        messagingTemplate.convertAndSend("/topic/serverInit", serversDetails);
    }

    public void sendServerDetails(String serverId, double load) {
//        Map<String, Double> serverDetails = new LinkedHashMap<>();
//        serverDetails.put(serverId, load);
//        messagingTemplate.convertAndSend("/topic/serverDetails", serverDetails);
    }

    public void sendServerDetails_(Map<String, Integer> serversLoad) {
        messagingTemplate.convertAndSend("/topic/serverDetails", serversLoad);
    }

    public void sendTotalTasks(Integer totalTasks) {
        messagingTemplate.convertAndSend("/topic/taskCompletionTotalTasks", totalTasks);
    }

    public void sendTaskCompletion(Map<String, Integer> taskCompletion) {
        messagingTemplate.convertAndSend("/topic/taskCompletion", taskCompletion);
    }
}
