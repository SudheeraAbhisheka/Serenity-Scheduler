package org.example.server1.service;

import com.example.KeyValueObject;
import lombok.Getter;

@Getter
public class TaskAssignment {
    private final KeyValueObject task;
    private final String serverId;
    private final double time;

    public TaskAssignment(KeyValueObject task, String serverId, double time) {
        this.task = task;
        this.serverId = serverId;
        this.time = time;
    }
}
