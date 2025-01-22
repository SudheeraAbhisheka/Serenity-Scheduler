package org.example.server1.service;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server1.component.Kafka_consumer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

@Service
public class LoadBalancingAlgorithm {
    private final RestTemplate restTemplate;
    private final ConcurrentLinkedQueue<KeyValueObject> wlbQueue;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public LoadBalancingAlgorithm(RestTemplate restTemplate, Kafka_consumer kafka_consumer) {
        this.restTemplate = restTemplate;
        this.wlbQueue = kafka_consumer.getWlbQueue();
    }

    public void weightedLoadBalancing(int wlbFixedRate){
        scheduler.scheduleAtFixedRate(() -> {
            ArrayList<KeyValueObject> tasks = new ArrayList<>();

            LinkedHashMap<String, Double> servers = restTemplate.exchange(
                    "http://servers:8084/api/get-servers",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<LinkedHashMap<String, Double>>() {}
            ).getBody();

            LinkedHashMap<String, Integer> remainingCaps = restTemplate.exchange(
                    "http://servers:8084/api/get-remaining-caps",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<LinkedHashMap<String, Integer>>() {}
            ).getBody();

            LinkedHashMap<String, Double> currentServerLoads = restTemplate.exchange(
                    "http://servers:8084/api/get-server-loads",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<LinkedHashMap<String, Double>>() {}
            ).getBody();

            assert remainingCaps != null;
            int totalRemainingCap = remainingCaps.values().stream().mapToInt(Integer::intValue).sum();

            while ((wlbQueue.peek()) != null && totalRemainingCap > 0 ) {
                tasks.add(wlbQueue.poll());

                totalRemainingCap--;
            }

            try {
                String url2 = "http://servers:8084/api/wlb-algorithm";
                restTemplate.postForEntity(url2, weightLoadBalancing(tasks, servers, remainingCaps, currentServerLoads), Map.class);
            } catch (Exception e) {
                System.err.printf("Error sending to wlb-algorithm: %s\n", e.getMessage());
            }

        }, 0, wlbFixedRate, TimeUnit.MILLISECONDS);
    }

    private Map<String, String> weightLoadBalancing(List<KeyValueObject> tasks, LinkedHashMap<String, Double> servers,
                                                    Map<String, Integer> remainingCaps, Map<String, Double> currentServerLoads) throws JsonProcessingException {
        Map<String, String> taskAssignments = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, Double> serverLoads = new HashMap<>();
        for (String serverId : servers.keySet()) {
            serverLoads.put(
                    serverId,
                    currentServerLoads.getOrDefault(serverId, 0.0)
            );
        }

        for (KeyValueObject task : tasks) {
            double taskWeight = task.getWeight();
            String bestServer = null;
            double bestCompletionTime = Double.MAX_VALUE;

            for (Map.Entry<String, Double> entry : servers.entrySet()) {
                String serverId = entry.getKey();

                if(remainingCaps.get(serverId) == 0){
                    continue;
                }

                double serverSpeed = entry.getValue();
                double currentLoad = serverLoads.get(serverId);

                double completionTime = currentLoad + (taskWeight / serverSpeed);
                if (completionTime < bestCompletionTime) {
                    bestCompletionTime = completionTime;
                    bestServer = serverId;
                }
            }

            double chosenServerSpeed = servers.get(bestServer);
            serverLoads.put(bestServer, serverLoads.get(bestServer) + (taskWeight / chosenServerSpeed));
            remainingCaps.put(bestServer, remainingCaps.get(bestServer) - 1);

            taskAssignments.put(objectMapper.writeValueAsString(task), bestServer);

        }
        return taskAssignments;
    }
}
