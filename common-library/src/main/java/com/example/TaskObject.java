package com.example;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class TaskObject {
    private String key;
    private int value;
    private int weight;
    private long generatedAt;
    @Setter
    private boolean executed;
    @Setter
    private int priority;
    @Setter
    private long startOfProcessAt;
    @Setter
    private long endOfProcessAt;
    @Setter
    private String serverKey;

    public TaskObject() {
    }

    public TaskObject(String key, int value, int weight, boolean executed, int priority, long generatedAt) {
        this.key = key;
        this.value = value;
        this.weight = weight;
        this.executed = executed;
        this.priority = priority;
        this.generatedAt = generatedAt;
    }
}
