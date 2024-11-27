package org.example.server1.service;

import com.example.KeyValueObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArrivedTimeObject {
    long arrivedTime;
    KeyValueObject keyValueObject;

    public ArrivedTimeObject(long arrivedTime, KeyValueObject keyValueObject) {
        this.arrivedTime = arrivedTime;
        this.keyValueObject = keyValueObject;
    }
}
