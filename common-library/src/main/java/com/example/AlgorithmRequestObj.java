package com.example;

import lombok.Getter;

@Getter
public class AlgorithmRequestObj {
    private String algorithm;
    private String messageBroker;

    public AlgorithmRequestObj() {
    }

    public AlgorithmRequestObj(String algorithm, String messageBroker) {
        this.algorithm = algorithm;
        this.messageBroker = messageBroker;
    }
}
