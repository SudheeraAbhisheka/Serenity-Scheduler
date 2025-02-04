package org.example.server1.service;

import com.example.TaskObject;
import lombok.Getter;

@Getter
public class OldObject {
    private final long age;
    private final TaskObject taskObject;

    public OldObject(long age, TaskObject taskObject) {
        this.age = age;
        this.taskObject = taskObject;
    }
}
