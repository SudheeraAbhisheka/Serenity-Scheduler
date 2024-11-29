package org.example.servers.service;

import com.example.KeyValueObject;
import lombok.Getter;

import java.util.concurrent.BlockingQueue;

@Getter
public class ServerObject {
    private final int serverId;
    private final BlockingQueue<KeyValueObject> queueServer;
    private final int serverSpeed;

    public ServerObject(int serverId, BlockingQueue<KeyValueObject> queueServer, int serverSpeed) {
        this.serverId = serverId;
        this.queueServer = queueServer;
        this.serverSpeed = serverSpeed;
    }
}
