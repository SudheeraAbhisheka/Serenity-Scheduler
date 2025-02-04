package org.example.servers.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.example.servers.entity.TaskEntity;

@Repository
public interface TaskRepository
        extends CassandraRepository<TaskEntity, String> {
    // By default, CassandraRepository gives you save(), findById(), etc.
}
