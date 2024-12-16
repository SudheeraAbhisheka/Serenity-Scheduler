package org.example.server1_terminal.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.example.server1_terminal.config.AppConfig;
import org.example.server1_terminal.service.Server1Service;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestTemplate;

public class AlgorithmSelectorApp extends Application {
    private Server1Service server1Service;

    @Override
    public void init() {
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        server1Service = context.getBean(Server1Service.class);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Algorithm Selector");

        Label algorithmLabel = new Label("Select a Scheduling Model:");
        Label brokerLabel = new Label("Select a Message Broker:");

        ComboBox<String> algorithmComboBox = new ComboBox<>();
        algorithmComboBox.getItems().addAll(
                "Priority-based Scheduling",
                "Complete-and-then-Fetch",
                "Weight-load-balancing"
        );

        ComboBox<String> brokerComboBox = new ComboBox<>();
        brokerComboBox.getItems().addAll("Kafka", "RabbitMQ");

        Button submitButton = new Button("Set Algorithm");

        Label responseLabel = new Label();

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(algorithmLabel, 0, 0);
        grid.add(algorithmComboBox, 1, 0);
        grid.add(brokerLabel, 0, 1);
        grid.add(brokerComboBox, 1, 1);
        grid.add(submitButton, 1, 2);
        grid.add(responseLabel, 1, 3);

        submitButton.setOnAction(event -> {
            String algorithm = algorithmComboBox.getValue();
            String messageBroker = brokerComboBox.getValue();

            if (algorithm != null && messageBroker != null) {
                String simpleAlgorithm = algorithm.toLowerCase().replace(" ", "-");
                String simpleMessageBroker = messageBroker.toLowerCase();

                boolean success = server1Service.setAlgorithm(simpleAlgorithm, simpleMessageBroker);

                if (success) {
                    responseLabel.setText("Algorithm set successfully - " + algorithm);
                } else {
                    responseLabel.setText("Failed to set algorithm.");
                }
            } else {
                responseLabel.setText("Please select both an algorithm and a message broker.");
            }
        });

        Scene scene = new Scene(grid, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
