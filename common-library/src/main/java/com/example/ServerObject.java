package com.example;

import lombok.Getter;

import java.util.concurrent.LinkedBlockingQueue;

@Getter
public class ServerObject {
    private String serverId;
    private LinkedBlockingQueue<TaskObject> queueServer;
    private double serverSpeed;

    public ServerObject() {
    }

    public ServerObject(String serverId, LinkedBlockingQueue<TaskObject> queueServer, double serverSpeed) {
        this.serverId = serverId;
        this.queueServer = queueServer;
        this.serverSpeed = serverSpeed;
    }
}
