package org.example.servers_terminal;

import javafx.application.Application;
import org.example.servers_terminal.gui.CombinedApplication;
import org.example.servers_terminal.gui.ServerConfigApp;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServersTerminalApplication {

    public static void main(String[] args) {
//        SpringApplication.run(ServersTerminalApplication.class, args);
        Application.launch(ServerConfigApp.class, args);
//        Application.launch(CombinedApplication.class, args);
    }

}
