package org.example.server1.controller;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.service.LoadBalancingAlgorithm;
import org.example.server1.service.PriorityBasedScheduling;
import org.example.server1.service.SchedulingAlgorithms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
@RequestMapping("/consumer-one")
public class ConsumerOneController {

    private final SchedulingAlgorithms schedulingAlgorithms;
    private final LoadBalancingAlgorithm loadBalancingAlgorithm;
    private final PriorityBasedScheduling priorityBasedScheduling;
    private final Kafka_consumer kafka_consumer;
    private String algorithmName;
    private final ServerControllerEmitter serverControllerEmitter;

    @Autowired
    public ConsumerOneController(SchedulingAlgorithms schedulingAlgorithms, Kafka_consumer kafka_consumer,
                                 LoadBalancingAlgorithm loadBalancingAlgorithm, ServerControllerEmitter serverControllerEmitter,
                                 PriorityBasedScheduling priorityBasedScheduling) {
        this.schedulingAlgorithms = schedulingAlgorithms;
        this.loadBalancingAlgorithm = loadBalancingAlgorithm;
        this.priorityBasedScheduling = priorityBasedScheduling;
        this.kafka_consumer = kafka_consumer;
        this.serverControllerEmitter = serverControllerEmitter;
    }

    @PostMapping("/set-complete-and-fetch")
    public void setCompleteAndFetch() {
        schedulingAlgorithms.setDynamicBlockingQueue(kafka_consumer.getBlockingQueueCompleteF());
        schedulingAlgorithms.executeCATF();
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
    public void setPriorityCompleteFetch(@RequestBody LinkedHashMap<Integer, Double> thresholdTime) {
        System.out.println("algorithm: priority-complete-fetch");
        priorityBasedScheduling.setBlockingQueuePriorityS(kafka_consumer.getBlockingQueuePriorityS());
        priorityBasedScheduling.priorityBasedScheduling(thresholdTime, "complete-fetch");
        schedulingAlgorithms.executeCATF();

        kafka_consumer.setSchedulingAlgorithm("age-based-priority-scheduling");
        algorithmName = "age-based-priority-scheduling";
        serverControllerEmitter.sendUpdate("Scheduling algorithm: age-based-priority-scheduling");
    }

    @PostMapping("/set-priority-load-balancing")
    public void setPriorityLoadBalancing(@RequestBody LinkedHashMap<Integer, Double> thresholdTime) {
        System.out.println("algorithm: priority-load-balancing");
        priorityBasedScheduling.setBlockingQueuePriorityS(kafka_consumer.getBlockingQueuePriorityS());
        priorityBasedScheduling.priorityBasedScheduling(thresholdTime, "load-balancing");
        loadBalancingAlgorithm.wlb_serverInit();
        loadBalancingAlgorithm.weightedLoadBalancing();

        kafka_consumer.setSchedulingAlgorithm("age-based-priority-scheduling");
        algorithmName = "age-based-priority-scheduling";
        serverControllerEmitter.sendUpdate("Scheduling algorithm: age-based-priority-scheduling");
    }

    @PostMapping("/notify-new-servers")
    public void addServers() {
        if(schedulingAlgorithms.getDynamicBlockingQueue() != null){
            schedulingAlgorithms.executeCATF();
        }
        if(loadBalancingAlgorithm.getWlbQueue() != null){
           loadBalancingAlgorithm.wlb_serverInit();
        }
    }

    @PostMapping("/empty-server")
    public void notifyEmptyServer(@RequestParam String serverId) {
        loadBalancingAlgorithm.setEmptyServerAvailable(true);

        synchronized (loadBalancingAlgorithm.getLock()){
            loadBalancingAlgorithm.getLock().notify();
        }
    }


    @PostMapping("/crashed-tasks")
    public void addCrashedTasks(@RequestParam Integer serverId, @RequestBody List<KeyValueObject> crashedTasks) throws InterruptedException {
        if(algorithmName.equals("complete-and-then-fetch")){
            schedulingAlgorithms.terminateServer(Integer.toString(serverId), crashedTasks, algorithmName);
        }
        else if(algorithmName.equals("weight-load-balancing") || algorithmName.equals("age-based-priority-scheduling")){
            loadBalancingAlgorithm.terminateServer(Integer.toString(serverId), crashedTasks, algorithmName);
        }
    }
}
