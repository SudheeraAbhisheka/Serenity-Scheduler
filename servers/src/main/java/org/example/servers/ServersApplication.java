package org.example.servers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ServersApplication {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(ServersApplication.class, args);
    }

    public static void restart() {
        Thread thread = new Thread(() -> {
            System.out.println("Restarting application context...");
            context.close(); // Close the current context
            context = SpringApplication.run(ServersApplication.class, new String[0]); // Restart
            System.out.println("Application context restarted.");
        });

        thread.setDaemon(false); // Ensure the thread is not a daemon to avoid JVM shutdown
        thread.start();
    }
}
