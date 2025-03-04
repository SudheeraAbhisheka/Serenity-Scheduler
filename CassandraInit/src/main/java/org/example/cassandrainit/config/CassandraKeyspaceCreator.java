package org.example.cassandrainit.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraKeyspaceCreator {

    @Bean
    public CommandLineRunner createKeyspace(CqlSession session) {
        return args -> {
            String createKeyspaceQuery = "CREATE KEYSPACE IF NOT EXISTS my_keyspace " +
                    "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1} " +
                    "AND durable_writes = true;";
            session.execute(createKeyspaceQuery);
            System.out.println("Keyspace 'my_keyspace' ensured.");

        };
    }
}
