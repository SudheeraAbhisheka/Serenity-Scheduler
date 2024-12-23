package org.example.servers_terminal.gui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.example.servers_terminal.service.Server1Service;
import org.springframework.context.ApplicationContext;

public class AlgorithmSelectorUI {
    private final Server1Service server1Service;

    public AlgorithmSelectorUI(ApplicationContext context) {
        this.server1Service = context.getBean(Server1Service.class);
    }

    public void show(Stage primaryStage) {
        primaryStage.setTitle("Algorithm Selector");

        Label algorithmLabel = new Label("Select a Scheduling Model:");
        Label brokerLabel = new Label("Select a Message Broker:");

        ComboBox<String> algorithmComboBox = new ComboBox<>();
        algorithmComboBox.getItems().addAll(
                "Age-based-priority-scheduling",
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

//                boolean success = server1Service.setAlgorithm(simpleAlgorithm, simpleMessageBroker);
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
}