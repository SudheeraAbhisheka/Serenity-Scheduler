package org.example.servers.entity;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import lombok.Data;

/**
 * This is our Cassandra table equivalent of KeyValueObject.
 */
@Table("key_value_object") // The table name in Cassandra
@Data
public class KeyValueObjectEntity {

    @PrimaryKey
    private String key;

    private int value;
    private int weight;
    private long generatedAt;
    private boolean executed;
    private int priority;
    private long startOfProcessAt;
    private long endOfProcessAt;
    private String serverKey;
}
