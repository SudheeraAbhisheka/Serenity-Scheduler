package org.example.server1.controller;

import com.example.AlgorithmRequestObj;
import com.example.KeyValueObject;
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
    public ResponseEntity<String> addCrashedTasks(@RequestParam Integer serverId, @RequestBody List<KeyValueObject> crashedTasks) {
        ResponseEntity<String> responseEntity;
        CopyOnWriteArrayList<KeyValueObject> crashedTasksThreadSafe = new CopyOnWriteArrayList<>();

        try {
            crashedTasksThreadSafe = new CopyOnWriteArrayList<>(crashedTasks);
            responseEntity = ResponseEntity.ok("Tasks received successfully");

        } catch (Exception e) {
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing tasks");
        }

        String serverIdString = serverId.toString();

        System.out.println("stopping server "+serverIdString);
        System.out.println(crashedTasksThreadSafe);
        System.out.println("current working task: "+schedulingAlgorithms.getCurrentWorkingTask1().get(serverIdString));

        KeyValueObject workingTask = schedulingAlgorithms.getCurrentWorkingTask1().remove(serverIdString);

        if(workingTask != null) {
            crashedTasksThreadSafe.add(workingTask);
        }

        schedulingAlgorithms.getServerSwitches().put(serverIdString, false);

        schedulingAlgorithms.getCrashedTasks().addAll(crashedTasksThreadSafe);

        return responseEntity;
    }
}
