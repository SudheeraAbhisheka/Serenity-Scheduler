/*
package org.example.servers.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
public class CassandraConfig {

    // Initial session without a keyspace for administrative tasks.
//    @Bean
//    public CqlSession initialSession() {
//        return CqlSession.builder()
//                .addContactPoint(new InetSocketAddress("cassandra", 9042))
//                .withLocalDatacenter("datacenter1")
//                .build();
//    }

    // Use a CommandLineRunner to create the keyspace.
    @Bean
    public CommandLineRunner createKeyspace(CqlSession initialSession) {
        return args -> {
            String createKeyspaceQuery = "CREATE KEYSPACE IF NOT EXISTS my_keyspace " +
                    "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};";
            initialSession.execute(createKeyspaceQuery);
            System.out.println("Keyspace 'my_keyspace' ensured.");
        };
    }

    @Bean
    public CqlSession keyspaceSession() {
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress("cassandra", 9042))
                .withLocalDatacenter("datacenter1")
                .withKeyspace("my_keyspace")
                .build();
    }

    @Bean
    public CommandLineRunner createTableQuery(CqlSession session) {
        return args -> {

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
