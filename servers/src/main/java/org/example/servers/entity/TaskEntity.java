package org.example.servers.entity;

import com.example.TaskObject;
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

    public TaskEntity() {
    }

    public TaskEntity(TaskObject task) {
        this.key = task.getKey();
        this.value = task.getValue();
        this.weight = task.getWeight();
        this.generatedAt = task.getGeneratedAt();
        this.executed = task.isExecuted();
        this.priority = task.getPriority();
        this.startOfProcessAt = task.getStartOfProcessAt();
        this.endOfProcessAt = task.getEndOfProcessAt();
        this.serverKey = task.getServerKey();
    }
}
