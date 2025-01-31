/*
package org.example.servers.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Service
public class CassandraKeyspaceCreator {

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.cassandra.local-datacenter}")
    private String datacenter;

    @PostConstruct
    public void createKeyspaceIfNotExists() {
        String url = String.format("jdbc:cassandra://%s:%d/", contactPoints, port);
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {

            String createKeyspaceQuery = String.format(
                    "CREATE KEYSPACE IF NOT EXISTS %s " +
                            "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};",
                    keyspace
            );
            statement.execute(createKeyspaceQuery);

            System.out.println("Keyspace verified/created: " + keyspace);

        } catch (Exception e) {
            System.err.println("Failed to verify/create keyspace: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
*/
