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

    public KeyValueObject() {
    }

    public KeyValueObject(String key, int value) {
        this.key = key;
        this.value = value;
    }
}
