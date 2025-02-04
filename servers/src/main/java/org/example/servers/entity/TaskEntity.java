package org.example.servers.entity;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import lombok.Data;

/**
 * This is our Cassandra table equivalent of KeyValueObject.
 */
@Table("tasks") // The table name in Cassandra
@Data
public class TaskEntity {

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
