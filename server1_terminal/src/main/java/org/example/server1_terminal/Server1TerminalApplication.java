package org.example.server1_terminal;

import javafx.application.Application;
import org.example.server1_terminal.gui.AlgorithmSelectorApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Server1TerminalApplication {

    public static void main(String[] args) {
//        SpringApplication.run(Server1TerminalApplication.class, args);
        Application.launch(AlgorithmSelectorApp.class, args);
    }

}
