package org.example.server1.controller;

import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.service.LoadBalancingAlgorithm;
import org.example.server1.service.SchedulingAlgorithms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/consumer-one")
public class ConsumerOneController {

    private final SchedulingAlgorithms schedulingAlgorithms;
    private final LoadBalancingAlgorithm loadBalancingAlgorithm;
    private final Kafka_consumer kafka_consumer;
    private String algorithmName;
    private final ServerControllerEmitter serverControllerEmitter;

    @Autowired
    public ConsumerOneController(SchedulingAlgorithms schedulingAlgorithms, Kafka_consumer kafka_consumer,
                                 LoadBalancingAlgorithm loadBalancingAlgorithm, ServerControllerEmitter serverControllerEmitter) {
        this.schedulingAlgorithms = schedulingAlgorithms;
        this.loadBalancingAlgorithm = loadBalancingAlgorithm;
        this.kafka_consumer = kafka_consumer;
        this.serverControllerEmitter = serverControllerEmitter;
    }

    @PostMapping("/set-complete-and-fetch")
    public void setCompleteAndFetch() {
        schedulingAlgorithms.executeCATF();
        kafka_consumer.setSchedulingAlgorithm("complete-and-then-fetch");
        algorithmName = "complete-and-then-fetch";
        serverControllerEmitter.sendUpdate("Scheduling algorithm: complete-and-then-fetch");
    }

    @PostMapping("/set-priority-scheduling")
    public void setPriorityBased(@RequestBody LinkedHashMap<Integer, Double> thresholdTime) {
        schedulingAlgorithms.executeCATF();
        schedulingAlgorithms.priorityBasedScheduling(thresholdTime);
        kafka_consumer.setSchedulingAlgorithm("age-based-priority-scheduling");
        algorithmName = "age-based-priority-scheduling";
        serverControllerEmitter.sendUpdate("Scheduling algorithm: age-based-priority-scheduling");
    }

    @PostMapping("/set-work-load-balancing")
    public void setWlbFixedRate_(@RequestBody int fixedRate) {
        loadBalancingAlgorithm.weightedLoadBalancing(fixedRate);
        kafka_consumer.setSchedulingAlgorithm("weight-load-balancing");
        algorithmName = "weight-load-balancing";
        serverControllerEmitter.sendUpdate("Scheduling algorithm: weight-load-balancing");
    }

    @PostMapping("/notify-new-servers")
    public void addServers() {
        if(schedulingAlgorithms.getDynamicBlockingQueue() != null){
            schedulingAlgorithms.executeCATF();
        }
    }

    @PostMapping("/crashed-tasks")
    public void addCrashedTasks(@RequestParam Integer serverId, @RequestBody List<KeyValueObject> crashedTasks) throws InterruptedException, JsonProcessingException {
        String serverIdString = serverId.toString();
        String message = "Crashed server: " + serverIdString + "\n";

        if(schedulingAlgorithms.getCurrentWorkingTask().containsKey(serverIdString)){
            crashedTasks.add(
                    schedulingAlgorithms.getCurrentWorkingTask().remove(serverIdString)
            );
        }

        message += "Number of tasks replaced: " + crashedTasks.size() + "\n";

        Future<?> future = schedulingAlgorithms.getServerTaskMap().get(serverId.toString());
        boolean successful;
        if (schedulingAlgorithms.getServerTaskMap().containsKey(serverId.toString())) {
            successful = future.cancel(true);
            if(successful) {
                schedulingAlgorithms.getServerTaskMap().remove(serverId.toString());
            }
            else{
                System.out.println("Failed to crash server " + serverId);
            }
        }

        schedulingAlgorithms.getServerSwitches().put(serverIdString, false);
        kafka_consumer.setCrashedTasks(true);

        switch(algorithmName){
            case "complete-and-then-fetch": {
                for(KeyValueObject task : crashedTasks) {
                    kafka_consumer.getBlockingQueueCompleteF().put(task);
                }

                break;
            }

            case "age-based-priority-scheduling": {
                for(KeyValueObject task : crashedTasks) {
                    kafka_consumer.getBlockingQueuePriorityS().add(task);
                }
                break;
            }

            case "weight-load-balancing" :
                for(KeyValueObject task : crashedTasks) {
                    kafka_consumer.getWlbQueue().add(task);
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithmName);

        }

        serverControllerEmitter.sendUpdate(message);

        kafka_consumer.setCrashedTasks(false);

        Object lock = kafka_consumer.getLock();

        synchronized (lock) {
            lock.notify();
        }
    }
}
