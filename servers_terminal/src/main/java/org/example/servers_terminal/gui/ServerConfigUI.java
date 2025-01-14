package org.example.servers_terminal.gui;

import com.example.SpeedAndCapObj;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.example.servers_terminal.service.ServerService;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ServerConfigUI {
    private final ServerService serverService;
    private TextArea logArea;
    private final GridPane root;


    public ServerConfigUI(ApplicationContext context) {
        this.serverService = context.getBean(ServerService.class);
        root = new GridPane();
        show();
    }

    public void show() {
        root.setPadding(new Insets(10));
        root.setHgap(10);
        root.setVgap(10);

        TextField noOfServersField = new TextField();
        noOfServersField.setPromptText("Number of Servers");
        TextField serverSpeedsField = new TextField();
        serverSpeedsField.setPromptText("Server Speeds");
        TextField serverQueueCapsField = new TextField();
        serverQueueCapsField.setPromptText("Server Queue Capacities");

        TextField serverSpeedField = new TextField();
        serverSpeedField.setPromptText("Server Speed");
        TextField serverQueueCapacityField = new TextField();
        serverQueueCapacityField.setPromptText("Server Queue Capacity");

        Button submitOneButton = new Button("Submit one");
        Button submitManyButton = new Button("Submit many");
        Button exitButton = new Button("Exit");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(5);

        root.add(noOfServersField, 0, 0);
        root.add(serverSpeedsField, 1, 0);
        root.add(serverQueueCapsField, 2, 0);
        root.add(submitManyButton, 3, 0);
        root.add(serverSpeedField, 0, 1);
        root.add(serverQueueCapacityField, 1, 1);
        root.add(submitOneButton, 2, 1);
        root.add(exitButton, 0, 2);
        root.add(logArea, 0, 3, 4, 1);


        submitOneButton.setOnAction(e -> {
            double serverSpeed = 0.0;
            int serverQueueCapacity = 0;

            try {
                serverSpeed = Double.parseDouble(serverSpeedField.getText());
            } catch (NumberFormatException ex) {
                appendLog("Invalid server speed");
                return;
            }

            try {
                serverQueueCapacity = Integer.parseInt(serverQueueCapacityField.getText());
            } catch (NumberFormatException ex) {
                appendLog("Invalid server queue capacity");
                return;
            }

            boolean success = serverService.setServersOneByOne(
                    new SpeedAndCapObj(serverSpeed, serverQueueCapacity)
            );
            appendLog("Server submitted: " + success + " with speed: " + serverSpeed +
                    " and queue capacity: " + serverQueueCapacity);
        });

        submitManyButton.setOnAction(e -> {
            try {
                int noOfServers = Integer.parseInt(noOfServersField.getText());
                double serversSpeed = Double.parseDouble(serverSpeedsField.getText());
                int serversQueueCap = Integer.parseInt(serverQueueCapsField.getText());

                boolean success = serverService.setServersDefault(noOfServers, new SpeedAndCapObj(serversSpeed, serversQueueCap));
                appendLog(noOfServers + " servers submitted: " + success);

            } catch (NumberFormatException ex) {
                appendLog("Invalid input in default fields");
            }
        });

        exitButton.setOnAction(e -> Platform.exit());

    }

    private void appendLog(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    public Pane getRoot() {
        return root;
    }
}
