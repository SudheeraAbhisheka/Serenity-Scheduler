package org.example.servers_terminal.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.example.servers_terminal.service.ServerService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class FxController {

    private final ServerService serverService;

    public FxController(ServerService serverService) {
        this.serverService = serverService;
    }

    public void start(Stage stage) {
        LinkedHashMap<String, Double> servers = new LinkedHashMap<>();

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        TextField keyField = new TextField();
        keyField.setPromptText("Server Key");
        TextField valueField = new TextField();
        valueField.setPromptText("Server Value");

        Button addButton = new Button("Add Server");
        Button submitButton = new Button("Submit");
        Button defaultButton = new Button("Use Default");
        Button exitButton = new Button("Exit");

        gridPane.add(keyField, 0, 0);
        gridPane.add(valueField, 1, 0);
        gridPane.add(addButton, 0, 1);
        gridPane.add(submitButton, 1, 1);
        gridPane.add(defaultButton, 2, 1);
        gridPane.add(exitButton, 3, 1);

        addButton.setOnAction(e -> {
            String key = keyField.getText();
            Double value;
            try {
                value = Double.parseDouble(valueField.getText());
            } catch (NumberFormatException ex) {
                System.err.println("Invalid value");
                return;
            }
            servers.put(key, value);
            keyField.clear();
            valueField.clear();
            System.out.println("Server Added: " + key + " - " + value);
        });

        submitButton.setOnAction(e -> {
            boolean success = serverService.setServers(servers);
            System.out.println("Servers submitted: " + success);
        });

        defaultButton.setOnAction(e -> {
            LinkedHashMap<String, Double> defaultServers = new LinkedHashMap<>() {{
                put("1", 100.0);
                put("2", 20.0);
                put("3", 20.0);
                put("4", 20.0);
            }};
            boolean success = serverService.setServers(defaultServers);
            System.out.println("Default servers submitted: " + success);
        });

        exitButton.setOnAction(e -> Platform.exit());

        Scene scene = new Scene(gridPane, 500, 200);
        stage.setScene(scene);
        stage.setTitle("Server Configuration");
        stage.show();
    }
}
