package org.example.servers_terminal.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.example.servers_terminal.config.AppConfig;
import org.example.servers_terminal.service.ServerService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class CombinedApplication extends Application {
    private ApplicationContext context;

    @Override
    public void init() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
    }

    @Override
    public void start(Stage primaryStage) {
        Stage generatingMessageStage = new Stage();
        GeneratingMessagesUI generatingMessagesUI = new GeneratingMessagesUI(context);
        generatingMessagesUI.show(generatingMessageStage);

        Stage algorithmStage = new Stage();
        AlgorithmSelectorUI algorithmUI = new AlgorithmSelectorUI(context);
        algorithmUI.show(algorithmStage);

        Stage serverStage = new Stage();
        ServerConfigUI serverUI = new ServerConfigUI(context);
        serverUI.show(serverStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
