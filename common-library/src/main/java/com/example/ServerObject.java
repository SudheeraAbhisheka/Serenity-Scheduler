package com.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
public class ServerObject {
    private String serverId;
    private LinkedBlockingQueue<KeyValueObject> queueServer;
    private double serverSpeed;

    public ServerObject() {
    }

    public ServerObject(String serverId, LinkedBlockingQueue<KeyValueObject> queueServer, double serverSpeed) {
        this.serverId = serverId;
        this.queueServer = queueServer;
        this.serverSpeed = serverSpeed;
    }
}
