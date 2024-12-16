package org.example.servers_terminal.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.example.servers_terminal.service.ServerService;

public class CombinedApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        new Thread(() -> {
            Platform.runLater(() -> {
                ServerConfigApp serverConfigApp = new ServerConfigApp();
                Stage stage1 = new Stage();
                serverConfigApp.start(stage1);
            });
        }).start();

        new Thread(() -> {
            Platform.runLater(() -> {
                AlgorithmSelectorApp algorithmSelectorApp = new AlgorithmSelectorApp();
                Stage stage2 = new Stage();
                try {
                    algorithmSelectorApp.init(); // Ensure init() is called
                    algorithmSelectorApp.start(stage2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
