package org.example.servers_terminal.gui;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.servers_terminal.service.Server1Service;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;

public class AlgorithmSelectorUI {
    private final Server1Service server1Service;
    private final Pane root;

    public AlgorithmSelectorUI(ApplicationContext context) {
        this.server1Service = context.getBean(Server1Service.class);
        root = new VBox();
        show();
    }

    public void show() {
        Label brokerLabel = new Label("Select a Message Broker:");
        Label algorithmLabel = new Label("Select a Scheduling Model:");
        algorithmLabel.setVisible(false);
        Label responseLabel = new Label();
        Label thresholdLabel = new Label("Add Threshold Times:");
        thresholdLabel.setVisible(false);
        Label wlbRateLabel = new Label("Enter Fixed Rate for Weight-load-balancing:");
        TextField wlbRateInput = new TextField();
        wlbRateLabel.setVisible(false);
        wlbRateInput.setVisible(false);

        VBox thresholdBox = new VBox(5);
        thresholdBox.setVisible(false);
        Button addThresholdButton = new Button("Add Threshold");
        addThresholdButton.setVisible(false);
        Button setAlgorithmButton = new Button("Set Algorithm");
        setAlgorithmButton.setVisible(false);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(brokerLabel, 0, 0);

        root.getChildren().add(grid);
        root.getChildren().add(thresholdBox);

        // Message Broker Buttons
        Button kafkaButton = new Button("Kafka");
        Button rabbitMQButton = new Button("RabbitMQ");
        grid.add(kafkaButton, 0, 1);
        grid.add(rabbitMQButton, 1, 1);

        // Algorithm Buttons (hidden initially)
        Button ageBasedButton = new Button("Age-based-priority-scheduling");
        Button completeFetchButton = new Button("Complete-and-then-Fetch");
        Button weightLoadButton = new Button("Weight-load-balancing");
        ageBasedButton.setVisible(false);
        completeFetchButton.setVisible(false);
        weightLoadButton.setVisible(false);
        grid.add(algorithmLabel, 0, 2);
        grid.add(ageBasedButton, 0, 3);
        grid.add(completeFetchButton, 1, 3);
        grid.add(weightLoadButton, 2, 3);

        grid.add(thresholdLabel, 0, 4);
        grid.add(thresholdBox, 0, 5, 2, 1);
        grid.add(addThresholdButton, 2, 5);
        grid.add(wlbRateLabel, 0, 6);
        grid.add(wlbRateInput, 1, 6);
        grid.add(setAlgorithmButton, 0, 7, 3, 1);
        grid.add(responseLabel, 0, 8, 3, 1);

        // Action handlers
        kafkaButton.setOnAction(event -> {
            algorithmLabel.setVisible(true);
            ageBasedButton.setVisible(true);
            completeFetchButton.setVisible(true);
            weightLoadButton.setVisible(true);
        });

        rabbitMQButton.setOnAction(event -> {
            algorithmLabel.setVisible(true);
            ageBasedButton.setVisible(true);
            completeFetchButton.setVisible(true);
            weightLoadButton.setVisible(true);
        });

        ageBasedButton.setOnAction(event -> {
            thresholdLabel.setVisible(true);
            thresholdBox.setVisible(true);
            addThresholdButton.setVisible(true);
            setAlgorithmButton.setVisible(true);
            wlbRateLabel.setVisible(false);
            wlbRateInput.setVisible(false);

            addThresholdButton.setOnAction(addEvent -> {
                HBox newThresholdRow = new HBox(5);
                Label keyLabel = new Label("Threshold " + (thresholdBox.getChildren().size() + 1) + ":");
                TextField valueField = new TextField();
                newThresholdRow.getChildren().addAll(keyLabel, valueField);
                thresholdBox.getChildren().add(newThresholdRow);
            });

            setAlgorithmButton.setOnAction(setEvent -> {
                LinkedHashMap<Integer, Double> thresholdTime = new LinkedHashMap<>();

                try {
                    for (int i = 0; i < thresholdBox.getChildren().size(); i++) {
                        HBox thresholdRow = (HBox) thresholdBox.getChildren().get(i);
                        TextField valueField = (TextField) thresholdRow.getChildren().get(1);
                        double value = Double.parseDouble(valueField.getText().trim());
                        thresholdTime.put(i + 1, value);
                    }
                    boolean success = server1Service.setMessageBroker("kafka"); // Replace with selected broker
                    boolean success2 = server1Service.setPriorityScheduling(thresholdTime);

                    responseLabel.setText(success && success2 ? "Algorithm set successfully - Age-based-priority-scheduling" : "Failed to set algorithm.");
                } catch (NumberFormatException e) {
                    responseLabel.setText("Invalid input for Threshold Times. Please enter valid numbers.");
                }
            });
        });

        completeFetchButton.setOnAction(event -> {
            thresholdLabel.setVisible(false);
            thresholdBox.setVisible(false);
            addThresholdButton.setVisible(false);
            setAlgorithmButton.setVisible(true);
            wlbRateLabel.setVisible(false);
            wlbRateInput.setVisible(false);

            setAlgorithmButton.setOnAction(setEvent -> {
                boolean success = server1Service.setMessageBroker("kafka"); // Replace with selected broker
                boolean success2 = server1Service.setCompleteAndFetch();

                responseLabel.setText(success && success2 ? "Algorithm set successfully - Complete-and-then-Fetch" : "Failed to set algorithm.");
            });
        });

        weightLoadButton.setOnAction(event -> {
            thresholdLabel.setVisible(false);
            thresholdBox.setVisible(false);
            addThresholdButton.setVisible(false);
            setAlgorithmButton.setVisible(true);
            wlbRateLabel.setVisible(true);
            wlbRateInput.setVisible(true);

            setAlgorithmButton.setOnAction(setEvent -> {
                try {
                    int wlbFixedRate = Integer.parseInt(wlbRateInput.getText().trim());
                    boolean success = server1Service.setMessageBroker("kafka"); // Replace with selected broker
                    boolean success2 = server1Service.setWorkLoadBalancing(wlbFixedRate);

                    responseLabel.setText(success && success2 ? "Algorithm set successfully - Weight-load-balancing" : "Failed to set algorithm.");
                } catch (NumberFormatException e) {
                    responseLabel.setText("Invalid input for Fixed Rate. Please enter a valid number.");
                }
            });
        });

    }

    public Pane getRoot() {
        return root;
    }
}
