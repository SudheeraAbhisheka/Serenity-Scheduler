package org.example.server1.controller;

import com.example.TaskObject;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.service.LoadBalancingAlgorithm;
import org.example.server1.service.PriorityBasedScheduling;
import org.example.server1.service.CompleteFetchAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/consumer-one")
public class ConsumerOneController {

    private final CompleteFetchAlgorithm completeFetchAlgorithm;
    private final LoadBalancingAlgorithm loadBalancingAlgorithm;
    private final PriorityBasedScheduling priorityBasedScheduling;
    private final Kafka_consumer kafka_consumer;
    private String algorithmName;
    private final ServerControllerEmitter serverControllerEmitter;

    private static final String COMPLETE_AND_THEN_FETCH = "complete-and-then-fetch";
    private static final String WEIGHT_LOAD_BALANCING = "weight-load-balancing";
    private static final String PRIORITY_COMPLETE_FETCH = "priority-complete-fetch";
    private static final String PRIORITY_LOAD_BALANCING = "priority-load-balancing";

    @Autowired
    public ConsumerOneController(CompleteFetchAlgorithm completeFetchAlgorithm, Kafka_consumer kafka_consumer,
                                 LoadBalancingAlgorithm loadBalancingAlgorithm, ServerControllerEmitter serverControllerEmitter,
                                 PriorityBasedScheduling priorityBasedScheduling) {
        this.completeFetchAlgorithm = completeFetchAlgorithm;
        this.loadBalancingAlgorithm = loadBalancingAlgorithm;
        this.priorityBasedScheduling = priorityBasedScheduling;
        this.kafka_consumer = kafka_consumer;
        this.serverControllerEmitter = serverControllerEmitter;
    }

    @PostMapping("/set-complete-and-fetch")
    public void setCompleteAndFetch() {
        completeFetchAlgorithm.setDynamicBlockingQueue(kafka_consumer.getBlockingQueueCompleteF());
        completeFetchAlgorithm.getRunningServers().clear();
        completeFetchAlgorithm.executeCATF();

        algorithmName = COMPLETE_AND_THEN_FETCH;
        kafka_consumer.setSchedulingAlgorithm(algorithmName);
        serverControllerEmitter.sendUpdate("Scheduling algorithm: " + algorithmName);
    }

    @PostMapping("/set-load-balancing")
    public void setLoadBalancing() {
        loadBalancingAlgorithm.setWlbQueue(kafka_consumer.getWlbQueue());
        loadBalancingAlgorithm.getRunningServers().clear();
        loadBalancingAlgorithm.wlb_serverInit();
        loadBalancingAlgorithm.weightedLoadBalancing();

        algorithmName = WEIGHT_LOAD_BALANCING;
        kafka_consumer.setSchedulingAlgorithm(algorithmName);
        serverControllerEmitter.sendUpdate("Scheduling algorithm: " + algorithmName);
    }

    @PostMapping("/set-priority-complete-fetch")
    public void setPriorityCompleteFetch(@RequestBody LinkedHashMap<Integer, Long> thresholdTime) {
        algorithmName = PRIORITY_COMPLETE_FETCH;
        priorityBasedScheduling.setBlockingQueuePriorityS(kafka_consumer.getBlockingQueuePriorityS());
        priorityBasedScheduling.priorityBasedScheduling(thresholdTime, algorithmName);
        completeFetchAlgorithm.getRunningServers().clear();
        completeFetchAlgorithm.executeCATF();
        setEmptyServer("");

        kafka_consumer.setSchedulingAlgorithm(algorithmName);
        serverControllerEmitter.sendUpdate("Scheduling algorithm: " + algorithmName);
    }

    @PostMapping("/set-priority-load-balancing")
    public void setPriorityLoadBalancing(@RequestBody LinkedHashMap<Integer, Long> thresholdTime) {
        algorithmName = PRIORITY_LOAD_BALANCING;
        priorityBasedScheduling.setBlockingQueuePriorityS(kafka_consumer.getBlockingQueuePriorityS());
        priorityBasedScheduling.priorityBasedScheduling(thresholdTime, algorithmName);
        loadBalancingAlgorithm.getRunningServers().clear();
        loadBalancingAlgorithm.wlb_serverInit();
        loadBalancingAlgorithm.weightedLoadBalancing();
        setEmptyServer("");

        kafka_consumer.setSchedulingAlgorithm(algorithmName);
        serverControllerEmitter.sendUpdate("Scheduling algorithm: " + algorithmName);
    }

    @PostMapping("/update-wait-time")
    public void updateLoadBalancingWaitTime(@RequestBody Long waitTime) {
        loadBalancingAlgorithm.setWaitingTime2(waitTime);
        priorityBasedScheduling.setWaitingTime2(waitTime);
    }

    @PostMapping("/notify-new-servers")
    public void addServers() {
        if(completeFetchAlgorithm.getDynamicBlockingQueue() != null){
            completeFetchAlgorithm.executeCATF();
        }
        if(loadBalancingAlgorithm.getWlbQueue() != null){
            loadBalancingAlgorithm.wlb_serverInit();
        }
    }

    @PostMapping("/set-empty-server")
    public void setEmptyServer(@RequestParam String serverId) {
        loadBalancingAlgorithm.setEmptyServerAvailable(true);

        synchronized (loadBalancingAlgorithm.getLock()){
            loadBalancingAlgorithm.getLock().notify();
        }
    }

    @PostMapping("/crashed-tasks")
    public void addCrashedTasks(@RequestParam Integer serverId, @RequestBody List<TaskObject> crashedTasks) throws InterruptedException {
        if(algorithmName.equals(COMPLETE_AND_THEN_FETCH) || algorithmName.equals(PRIORITY_COMPLETE_FETCH)){
            completeFetchAlgorithm.terminateServer(Integer.toString(serverId), crashedTasks, algorithmName);
        }
        else if(algorithmName.equals(WEIGHT_LOAD_BALANCING) || algorithmName.equals(PRIORITY_LOAD_BALANCING)){
            loadBalancingAlgorithm.terminateServer(Integer.toString(serverId), crashedTasks, algorithmName);
        }
    }
}
