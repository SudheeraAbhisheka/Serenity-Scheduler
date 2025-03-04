package org.example.servers_terminal.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class GeneratingMessagesUI {
    private final RestTemplate restTemplate;
    @Getter
    private final Pane root;

    public GeneratingMessagesUI(ApplicationContext context) {
        this.restTemplate = context.getBean(RestTemplate.class);
        root = new VBox();
        show();
    }

    public void show() {
        root.setPadding(new Insets(10));

        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefHeight(400);


        ComboBox<String> messageBrokerComboBox = new ComboBox<>(FXCollections.observableArrayList("kafka", "rabbitmq"));
        messageBrokerComboBox.setValue("kafka");

        TextField minWeightField = new TextField("1");
        minWeightField.setPromptText("Enter Min Weight");

        TextField maxWeightField = new TextField("10");
        maxWeightField.setPromptText("Enter Max Weight");

        TextField minPriorityField = new TextField("1");
        minPriorityField.setPromptText("Enter Min Priority");

        TextField maxPriorityField = new TextField("3");
        maxPriorityField.setPromptText("Enter Max Priority");

        TextField noOfTasksField = new TextField();
        noOfTasksField.setPromptText("Enter No. of Tasks");

        TextField scheduleRateField = new TextField();
        scheduleRateField.setPromptText("Enter Schedule Rate");

        TextField executionDurationField = new TextField();
        executionDurationField.setPromptText("Enter Execution Duration");

        Button startButton = new Button("Start");
        Button scheduleButton = new Button("Set schedule");
        Button clearButton = new Button("Clear Terminal");

        startButton.setOnAction(event -> {
            int noOfTasks = 0;
            int minWeight = 0;
            int maxWeight = 0;
            int minPriority = 0;
            int maxPriority = 0;

            String selectedBroker = messageBrokerComboBox.getValue();
            postRequest("8080/kafka-server/set-broker", selectedBroker);

            try {
                noOfTasks = Integer.parseInt(noOfTasksField.getText());
                minWeight = Integer.parseInt(minWeightField.getText());
                maxWeight = Integer.parseInt(maxWeightField.getText());
                minPriority = Integer.parseInt(minPriorityField.getText());
                maxPriority = Integer.parseInt(maxPriorityField.getText());
            } catch (NumberFormatException e) {
                outputArea.appendText("Please enter valid numbers for threads, tasks, weight, and priority.\n");
                return;
            }

            if (maxWeight < minWeight || maxPriority < minPriority) {
                outputArea.appendText("Please ensure max >= min for weight/priority.\n");
                return;
            }

            Map<String, Integer> map = new HashMap<>(Map.of(
                    "noOfTasks", noOfTasks,
                    "minWeight", minWeight,
                    "maxWeight", maxWeight,
                    "minPriority", minPriority,
                    "maxPriority", maxPriority)
            );

            postRequest("8084/api/set-no-of-tasks", noOfTasks);
            boolean tasksSet = postRequest("8080/kafka-server/set-message", map);
            if (tasksSet) {
                outputArea.appendText("Successfully set number of tasks: " + noOfTasks
                        + ", message broker: " + selectedBroker + "\n");
            } else {
                outputArea.appendText("Failed to set number of tasks: " + noOfTasks + "\n");
            }
        });

        scheduleButton.setOnAction(event -> {
            int scheduleRate = 0;
            int executionDuration = 0;
            int minWeight = 0;
            int maxWeight = 0;
            int minPriority = 0;
            int maxPriority = 0;

            String selectedBroker = messageBrokerComboBox.getValue();
            postRequest("8080/kafka-server/set-broker", selectedBroker);

            try {
                scheduleRate = Integer.parseInt(scheduleRateField.getText());
                executionDuration = Integer.parseInt(executionDurationField.getText());
                minWeight = Integer.parseInt(minWeightField.getText());
                maxWeight = Integer.parseInt(maxWeightField.getText());
                minPriority = Integer.parseInt(minPriorityField.getText());
                maxPriority = Integer.parseInt(maxPriorityField.getText());
            } catch (NumberFormatException e) {
                outputArea.appendText("Please enter valid numbers for threads, tasks, weight, and priority.\n");
                return;
            }

            if (maxWeight < minWeight || maxPriority < minPriority) {
                outputArea.appendText("Please ensure max >= min for weight/priority.\n");
                return;
            }

            Map<String, Integer> scheduledMap = new HashMap<>(Map.of(
                    "scheduleRate", scheduleRate,
                    "executionDuration", executionDuration,
                    "minWeight", minWeight,
                    "maxWeight", maxWeight,
                    "minPriority", minPriority,
                    "maxPriority", maxPriority)
            );

            int noOfTasks = (executionDuration * 1000) / scheduleRate;
            postRequest("8084/api/set-no-of-tasks", noOfTasks);
            boolean tasksSet = postRequest("8080/kafka-server/set-message-scheduled", scheduledMap);
            if (tasksSet) {
                outputArea.appendText("Successfully set number of tasks: " + noOfTasks
                        + ", message broker: " + selectedBroker + "\n");
            } else {
                outputArea.appendText("Failed to set number of tasks: " + noOfTasks + "\n");
            }
        });

        clearButton.setOnAction(event -> outputArea.clear());

        HBox row1 = new HBox(10);
        row1.setPadding(new Insets(10));
        row1.getChildren().addAll(
                new Label("Min weight:"),
                minWeightField,
                new Label("Max weight:"),
                maxWeightField,
                new Label("Min priority:"),
                minPriorityField,
                new Label("Max priority:"),
                maxPriorityField
        );

        HBox row2 = new HBox(10);
        row2.setPadding(new Insets(10));
        row2.getChildren().addAll(
                new Label("No of tasks:"),
                noOfTasksField,
                new Label("Message Broker:"),
                messageBrokerComboBox,
                startButton
        );

        HBox row3 = new HBox(10);
        row3.setPadding(new Insets(10));
        row3.getChildren().addAll(
                new Label("Schedule Rate(mills):"),
                scheduleRateField,
                new Label("Execution Duration(s):"),
                executionDurationField,
                scheduleButton,
                clearButton
        );

        root.getChildren().addAll(row1, row2, row3, outputArea);
    }

    private <T> boolean postRequest(String suffix, T payload) {
        String url = "http://localhost:" + suffix;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<T> request = new HttpEntity<>(payload, headers);
        try {
            restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
