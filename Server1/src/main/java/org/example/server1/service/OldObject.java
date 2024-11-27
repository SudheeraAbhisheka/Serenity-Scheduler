package org.example.server1.service;

import com.example.KeyValueObject;
import lombok.Getter;
import lombok.Setter;

@Getter
public class OldObject {
    private final long age;
    private final KeyValueObject keyValueObject;

    public OldObject(long age, KeyValueObject keyValueObject) {
        this.age = age;
        this.keyValueObject = keyValueObject;
    }
}
