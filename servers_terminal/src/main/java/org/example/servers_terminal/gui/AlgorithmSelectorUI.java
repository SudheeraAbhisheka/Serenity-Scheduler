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

import java.util.LinkedHashMap;

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

                boolean success = server1Service.setMessageBroker(simpleMessageBroker);
                boolean success2 = false;

                switch (simpleAlgorithm){
                    case "complete-and-then-fetch":{
                        success2 = server1Service.setCompleteAndFetch();
                    }
                    break;

                    case "age-based-priority-scheduling":{
                        LinkedHashMap<Integer, Double> thresholdTime = new LinkedHashMap<>();

                        thresholdTime.put(1, 10.0);
                        thresholdTime.put(2, 15.0);
                        thresholdTime.put(3, 20.0);

                        success2 = server1Service.setPriorityScheduling(thresholdTime);
                    }
                    break;

                    case "weight-load-balancing":{
                        success2 = server1Service.setWorkLoadBalancing(5000);
                    }
                }


                if (success && success2) {
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