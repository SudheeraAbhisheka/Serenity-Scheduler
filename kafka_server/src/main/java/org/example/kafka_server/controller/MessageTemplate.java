package org.example.kafka_server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageTemplate {
    @JsonProperty("name")
    private String name;
    @JsonProperty("processingTime")
    private int processingTime;
    @JsonProperty("numbers")
    private int[] numbers;

    public MessageTemplate(String name, int processingTime, int[] numbers) {
        this.name = name;
        this.processingTime = processingTime;
        this.numbers = numbers;
    }
}
