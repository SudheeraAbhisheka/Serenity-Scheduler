package org.example.servers_terminal;

import javafx.application.Application;
import org.example.servers_terminal.gui.JavaFxApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServersTerminalApplication {

    public static void main(String[] args) {
//        SpringApplication.run(ServersTerminalApplication.class, args);
        Application.launch(JavaFxApp.class, args);
    }

}
