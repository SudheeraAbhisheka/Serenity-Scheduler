package org.example.server1.controller;

import org.example.server1.component.ConsumerOne;
import org.example.server1.service.SchedulingAlgorithms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consumer-one")
public class ConsumerOneController {

    private final SchedulingAlgorithms schedulingAlgorithms;
    private final ConsumerOne consumerOne;

    @Autowired
    public ConsumerOneController(SchedulingAlgorithms schedulingAlgorithms, ConsumerOne consumerOne) {
        this.schedulingAlgorithms = schedulingAlgorithms;
        this.consumerOne = consumerOne;
    }

    @PostMapping("/set-algorithm")
    public void setAlgorithm(@RequestBody String schedulingAlgorithm) {
        schedulingAlgorithms.setSchedulingAlgorithm(schedulingAlgorithm);
        consumerOne.setSchedulingAlgorithm(schedulingAlgorithm);
    }
}
