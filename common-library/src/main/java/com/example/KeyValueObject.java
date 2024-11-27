package com.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyValueObject {
    @JsonProperty("key") private String key;
    @JsonProperty("value") private int value;
    @JsonProperty("weight") private int weight;
    @JsonProperty("executed") private boolean executed;
    @JsonProperty("priority") private int priority;

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
