package org.example.server1.service;

import com.example.KeyValueObject;
import lombok.Data;

import java.util.*;

@Data
public class WightedLBObject {
    private String serverId;
    private Double serverSpeed;
    private KeyValueObject keyValueObject;
    private double time;

    public WightedLBObject(String serverId, Double serverSpeed, KeyValueObject keyValueObject) {
        this.serverId = serverId;
        this.serverSpeed = serverSpeed;
        this.keyValueObject = keyValueObject;
    }

    public void weightLoadBalancing(List<KeyValueObject> tasks, LinkedHashMap<String, Double> servers) {
        Map<KeyValueObject, String> taskServerMap = new HashMap<>();

        int numTasks = tasks.size();
        int numServers = servers.size();

        List<Map.Entry<String, Double>> serverList = new ArrayList<>(servers.entrySet());

        double[] serverLoads = new double[numServers];
        Arrays.fill(serverLoads, 0.0);

        for (KeyValueObject task : tasks) {
            double taskWeight = task.getWeight();

            double bestCompletionTime = Double.MAX_VALUE;
            int bestServerIndex = -1;

            for (int i = 0; i < numServers; i++) {
                double serverSpeed = serverList.get(i).getValue();
                double currentServerLoad = serverLoads[i];

                double completionTime = currentServerLoad + (taskWeight / serverSpeed);

                if (completionTime < bestCompletionTime) {
                    bestCompletionTime = completionTime;
                    bestServerIndex = i;
                }
            }

            String chosenServerId = serverList.get(bestServerIndex).getKey();
            taskServerMap.put(task, chosenServerId);

            double chosenServerSpeed = serverList.get(bestServerIndex).getValue();
            serverLoads[bestServerIndex] += (taskWeight / chosenServerSpeed);
        }

        System.out.println("Task to Server Mapping: " + taskServerMap);
    }


}
