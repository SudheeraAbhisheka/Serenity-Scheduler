package org.example.servers_terminal.gui;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.example.servers_terminal.service.ServerService;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;

public class ServerConfigUI {
    private final ServerService serverService;
    private TextArea logArea;

    public ServerConfigUI(ApplicationContext context) {
        this.serverService = context.getBean(ServerService.class);
    }

    public void show(Stage primaryStage) {
        primaryStage.setTitle("Server Configuration");

        LinkedHashMap<String, Double> servers = new LinkedHashMap<>();
        int defaultQueueCapacity = 1;
        final IntegerProperty queueCapacity = new SimpleIntegerProperty(defaultQueueCapacity);

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        TextField keyField = new TextField();
        keyField.setPromptText("Server Key");
        TextField valueField = new TextField();
        valueField.setPromptText("Server Value");
        TextField queueCapacityField = new TextField();
        queueCapacityField.setPromptText("Queue Capacity");
        queueCapacityField.setText(String.valueOf(defaultQueueCapacity));

        Button addButton = new Button("Add Server");
        Button submitButton = new Button("Submit");
        Button defaultButton = new Button("Use Default");
        Button exitButton = new Button("Exit");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(5);

        gridPane.add(keyField, 0, 0);
        gridPane.add(valueField, 1, 0);
        gridPane.add(queueCapacityField, 2, 0);
        gridPane.add(addButton, 0, 1);
        gridPane.add(submitButton, 1, 1);
        gridPane.add(defaultButton, 2, 1);
        gridPane.add(exitButton, 3, 1);
        gridPane.add(logArea, 0, 2, 4, 1); // spanning all columns

        addButton.setOnAction(e -> {
            String key = keyField.getText();
            Double value;
            try {
                value = Double.parseDouble(valueField.getText());
            } catch (NumberFormatException ex) {
                appendLog("Invalid value for server");
                return;
            }
            servers.put(key, value);
            keyField.clear();
            valueField.clear();
            appendLog("Server Added: " + key + " - " + value);
        });

        submitButton.setOnAction(e -> {
            try {
                int newQueueCapacity = Integer.parseInt(queueCapacityField.getText());
                queueCapacity.set(newQueueCapacity);
            } catch (NumberFormatException ex) {
                appendLog("Invalid queue capacity");
                return;
            }
            boolean success = serverService.setServers(queueCapacity.get(), servers);
            appendLog("Servers submitted: " + success + " with queue capacity: " + queueCapacity.get());
        });

        defaultButton.setOnAction(e -> {
            LinkedHashMap<String, Double> defaultServers = new LinkedHashMap<>() {{
                put("1", 0.1);
                put("2", 0.1);
                put("3", 0.1);
                put("4", 0.1);
            }};
            try {
                int newQueueCapacity = Integer.parseInt(queueCapacityField.getText());
                queueCapacity.set(newQueueCapacity);
            } catch (NumberFormatException ex) {
                appendLog("Invalid queue capacity, using default: " + defaultQueueCapacity);
            }
            boolean success = serverService.setServers(queueCapacity.get(), defaultServers);
            appendLog("Default servers submitted: " + success + " with queue capacity: " + queueCapacity.get());
        });

        exitButton.setOnAction(e -> Platform.exit());

        Scene scene = new Scene(gridPane, 600, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void appendLog(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }
}
