package org.example.server1.controller;

import com.example.AlgorithmRequestObj;
import com.example.KeyValueObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.server1.component.Kafka_consumer;
import org.example.server1.component.RabbitMQ_consumer;
import org.example.server1.service.SchedulingAlgorithms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/consumer-one")
public class ConsumerOneController {

    private final SchedulingAlgorithms schedulingAlgorithms;
    private final RabbitMQ_consumer rabbitMQ_consumer;
    private final Kafka_consumer kafka_consumer;
    private String messageBroker;

    @Autowired
    public ConsumerOneController(SchedulingAlgorithms schedulingAlgorithms, RabbitMQ_consumer rabbitMQ_consumer, Kafka_consumer kafka_consumer) {
        this.schedulingAlgorithms = schedulingAlgorithms;
        this.rabbitMQ_consumer = rabbitMQ_consumer;
        this.kafka_consumer = kafka_consumer;
    }

    @PostMapping("/set-message-broker")
    public void setMessageBroker(@RequestBody String messageBroker) {
        this.messageBroker = messageBroker;
        schedulingAlgorithms.setMessageBroker(messageBroker);
    }

    @PostMapping("/set-complete-and-fetch")
    public void setCompleteAndFetch() {
        schedulingAlgorithms.setSchedulingAlgorithm("complete-and-then-fetch");
        schedulingAlgorithms.executeCATF();

        if(messageBroker.equals("kafka")){
            kafka_consumer.setSchedulingAlgorithm("complete-and-then-fetch");

            System.out.println("messageBroker = "+messageBroker);
        }
        else if(messageBroker.equals("rabbitmq")){
            rabbitMQ_consumer.setSchedulingAlgorithm("complete-and-then-fetch");
        }
    }

    @PostMapping("/set-priority-scheduling")
    public void setPriorityBased(@RequestBody LinkedHashMap<Integer, Double> thresholdTime) {
        schedulingAlgorithms.priorityBasedScheduling(thresholdTime);

        if(messageBroker.equals("kafka")){
            kafka_consumer.setSchedulingAlgorithm("age-based-priority-scheduling");
        }
        else if(messageBroker.equals("rabbitmq")){
            rabbitMQ_consumer.setSchedulingAlgorithm("age-based-priority-scheduling");
        }
    }

    @PostMapping("/set-work-load-balancing")
    public void setWlbFixedRate_(@RequestBody int fixedRate) {
        schedulingAlgorithms.weightedLoadBalancing(fixedRate);

        if(messageBroker.equals("kafka")){
            kafka_consumer.setSchedulingAlgorithm("weight-load-balancing");
        }
        else if(messageBroker.equals("rabbitmq")){
            rabbitMQ_consumer.setSchedulingAlgorithm("weight-load-balancing");
        }
    }

    @PostMapping("/notify-new-servers")
    public void addServers() {
        schedulingAlgorithms.notifyNewServersCATFModel();
        System.out.println("Notified new servers");
    }

    @GetMapping("/get-server1-details")
    public AlgorithmRequestObj server1Details() {
        return new AlgorithmRequestObj(
                schedulingAlgorithms.getSchedulingAlgorithm(),
                messageBroker
        );
    }

    @PostMapping("/crashed-tasks")
    public void addCrashedTasks(@RequestParam Integer serverId, @RequestBody List<KeyValueObject> crashedTasks) throws InterruptedException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        String serverIdString = serverId.toString();

        if(schedulingAlgorithms.getCurrentWorkingTask1().containsKey(serverIdString)){
            crashedTasks.add(
                    schedulingAlgorithms.getCurrentWorkingTask1().remove(serverIdString)
            );
        }

        Future<?> future = schedulingAlgorithms.getServerTaskMap().get(serverId.toString());
        boolean successful = false;
        if (future == null) {
            System.out.println(future);
        }
        else{
            successful = future.cancel(true);
            if(successful) {
                schedulingAlgorithms.getServerTaskMap().remove(serverId.toString());
            }
            else{
                System.out.println("Failed to crash server " + serverId);
            }
        }

        schedulingAlgorithms.getServerSwitches().put(serverIdString, false);
        Kafka_consumer.setCrashedTasks(true);
        System.out.println("Crashed tasks: "+crashedTasks.stream().map(KeyValueObject::getKey).toList());

        for(KeyValueObject task : crashedTasks) {
            System.out.println("Before put: "+task.getKey());
            schedulingAlgorithms.getDynamicBlockingQueue().put(
                    objectMapper.writeValueAsString(task)
            );
            System.out.println("After put: "+task.getKey());
        }

        Kafka_consumer.setCrashedTasks(false);

        Object lock = Kafka_consumer.getLock();

        synchronized (lock) {
            lock.notify();
        }
    }
}
