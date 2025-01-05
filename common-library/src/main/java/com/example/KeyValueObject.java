package com.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
public class KeyValueObject {
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

    public KeyValueObject() {
    }

    public KeyValueObject(String key, int value, int weight, boolean executed, int priority, long generatedAt) {
        this.key = key;
        this.value = value;
        this.weight = weight;
        this.executed = executed;
        this.priority = priority;
        this.generatedAt = generatedAt;
    }

    @Override
    public String toString() {
        return "KeyValueObject{" +
                "key='" + key + '\'' +
                ", value=" + value +
                ", weight=" + weight +
                ", generatedAt=" + generatedAt +
                ", executed=" + executed +
                ", priority=" + priority +
                ", startOfProcessAt=" + startOfProcessAt +
                ", endOfProcessAt=" + endOfProcessAt +
                ", serverKey='" + serverKey + '\'' +
                '}';
    }
}
