package com.example;

import lombok.Data;

@Data
public class SpeedAndCapObj {
    private double speed;
    private int cap;

    public SpeedAndCapObj() {
    }

    public SpeedAndCapObj(double speed, int cap) {
        this.speed = speed;
        this.cap = cap;
    }
}
