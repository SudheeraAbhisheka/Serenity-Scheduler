package com.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeyValueObject {
    @JsonProperty("key")
    private String key;
    @JsonProperty("value")
    private int value;
    @JsonProperty("load")
    private int load;
    @JsonProperty("executed")
    private boolean executed;

    public KeyValueObject() {
    }

    public KeyValueObject(String key, int value, int load, boolean executed) {
        this.key = key;
        this.value = value;
        this.load = load;
        this.executed = executed;
    }
}
