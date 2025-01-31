package org.example.servers.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;
import org.example.servers.entity.KeyValueObjectEntity;

@Repository
public interface KeyValueObjectRepository
        extends CassandraRepository<KeyValueObjectEntity, String> {
    // By default, CassandraRepository gives you save(), findById(), etc.
}
