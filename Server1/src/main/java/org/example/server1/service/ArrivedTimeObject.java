package org.example.server1.service;

import com.example.TaskObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArrivedTimeObject {
    long arrivedTime;
    TaskObject taskObject;

    public ArrivedTimeObject(long arrivedTime, TaskObject taskObject) {
        this.arrivedTime = arrivedTime;
        this.taskObject = taskObject;
    }
}
