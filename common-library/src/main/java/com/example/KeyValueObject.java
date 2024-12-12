package com.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KeyValueObject {
    private String key;
    private int value;
    private int weight;
    private boolean executed;
    private int priority;

    public KeyValueObject() {
    }

    public KeyValueObject(String key, int value, int weight, boolean executed, int priority) {
        this.key = key;
        this.value = value;
        this.weight = weight;
        this.executed = executed;
        this.priority = priority;
    }
}
