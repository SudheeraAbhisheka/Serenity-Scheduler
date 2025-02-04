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
        completeFetchAlgorithm.executeCATF();
        kafka_consumer.setSchedulingAlgorithm("complete-and-then-fetch");
        algorithmName = "complete-and-then-fetch";
        serverControllerEmitter.sendUpdate("Scheduling algorithm: complete-and-then-fetch");
    }

    @PostMapping("/set-load-balancing")
    public void setLoadBalancing() {
        loadBalancingAlgorithm.setWlbQueue(kafka_consumer.getWlbQueue());
        loadBalancingAlgorithm.wlb_serverInit();
        loadBalancingAlgorithm.weightedLoadBalancing();
        kafka_consumer.setSchedulingAlgorithm("weight-load-balancing");
        algorithmName = "weight-load-balancing";
        serverControllerEmitter.sendUpdate("Scheduling algorithm: weight-load-balancing");
    }

    @PostMapping("/set-priority-complete-fetch")
    public void setPriorityCompleteFetch(@RequestBody LinkedHashMap<Integer, Long> thresholdTime) {
        priorityBasedScheduling.setBlockingQueuePriorityS(kafka_consumer.getBlockingQueuePriorityS());
        priorityBasedScheduling.priorityBasedScheduling(thresholdTime, "complete-fetch");
        completeFetchAlgorithm.executeCATF();

        kafka_consumer.setSchedulingAlgorithm("age-based-priority-scheduling");
        algorithmName = "age-based-priority-scheduling";
        serverControllerEmitter.sendUpdate("Scheduling algorithm: age-based-priority-scheduling");
    }

    @PostMapping("/set-priority-load-balancing")
    public void setPriorityLoadBalancing(@RequestBody LinkedHashMap<Integer, Long> thresholdTime) {
        priorityBasedScheduling.setBlockingQueuePriorityS(kafka_consumer.getBlockingQueuePriorityS());
        priorityBasedScheduling.priorityBasedScheduling(thresholdTime, "load-balancing");
        loadBalancingAlgorithm.wlb_serverInit();
        loadBalancingAlgorithm.weightedLoadBalancing();

        kafka_consumer.setSchedulingAlgorithm("age-based-priority-scheduling");
        algorithmName = "age-based-priority-scheduling";
        serverControllerEmitter.sendUpdate("Scheduling algorithm: age-based-priority-scheduling");
    }

//    @PostMapping("/update-load-balancing-wait-times")
//    public void updateLoadBalancingWaitTimes(@RequestBody LinkedHashMap<String, Long> waitTimes) {
//        loadBalancingAlgorithm.setWaitingTime1(waitTimes.get("waitTime1"));
//        loadBalancingAlgorithm.setWaitingTime2(waitTimes.get("waitTime2"));
//    }

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
        if(algorithmName.equals("complete-and-then-fetch")){
            completeFetchAlgorithm.terminateServer(Integer.toString(serverId), crashedTasks, algorithmName);
        }
        else if(algorithmName.equals("weight-load-balancing") || algorithmName.equals("age-based-priority-scheduling")){
            loadBalancingAlgorithm.terminateServer(Integer.toString(serverId), crashedTasks, algorithmName);
        }
    }
}
