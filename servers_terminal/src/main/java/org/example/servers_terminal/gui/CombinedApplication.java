package org.example.servers_terminal.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
        TabPane tabPane = new TabPane();

        Tab serverConfigTab = new Tab("Server Configuration");
        ServerConfigUI serverUI = new ServerConfigUI(context);
        serverConfigTab.setContent(serverUI.getRoot());
        serverConfigTab.setClosable(false);
        tabPane.getTabs().add(serverConfigTab);

        Tab algorithmSelectorTab = new Tab("Algorithm Selector");
        AlgorithmSelectorUI algorithmUI = new AlgorithmSelectorUI(context);
        algorithmSelectorTab.setContent(algorithmUI.getRoot());
        algorithmSelectorTab.setClosable(false);
        tabPane.getTabs().add(algorithmSelectorTab);

        Tab generatingMessagesTab = new Tab("Generating Messages");
        GeneratingMessagesUI generatingMessagesUI = new GeneratingMessagesUI(context);
        generatingMessagesTab.setContent(generatingMessagesUI.getRoot());
        generatingMessagesTab.setClosable(false);
        tabPane.getTabs().add(generatingMessagesTab);

        Scene scene = new Scene(tabPane, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Unified UI");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
