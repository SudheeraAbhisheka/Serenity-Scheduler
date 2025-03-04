/*
package org.example.servers.config;

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

            String createTableQuery =
                    "CREATE TABLE IF NOT EXISTS my_keyspace.tasks (" +
                            "   key text PRIMARY KEY, " +
                            "   value int, " +
                            "   weight int, " +
                            "   generatedAt timestamp, " +
                            "   executed boolean, " +
                            "   priority int, " +
                            "   startOfProcessAt timestamp, " +
                            "   endOfProcessAt timestamp, " +
                            "   serverKey text" +
                            ");";
            session.execute(createTableQuery);

            System.out.println("Keyspace 'my_keyspace' and table 'tasks' ensured.");
        };
    }
}
*/
