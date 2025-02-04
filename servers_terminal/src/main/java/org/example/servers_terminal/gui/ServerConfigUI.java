package org.example.servers_terminal.gui;

import com.example.SpeedAndCapObj;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.example.servers_terminal.service.ServerService;
import org.springframework.context.ApplicationContext;

import java.util.Map;

public class ServerConfigUI {
    private final ServerService serverService;
    private TextArea logArea;
    private final Pane root;

    public ServerConfigUI(ApplicationContext context) {
        this.serverService = context.getBean(ServerService.class);
        root = new VBox();
        show();
    }

    public void show() {
        root.setPadding(new Insets(10));

        TextField heartBeatCheckingField = new TextField("3000");
        heartBeatCheckingField.setPromptText("Heartbeat Checking");
        TextField heartBeatMakingField = new TextField("1000");
        heartBeatMakingField.setPromptText("Heartbeat Making");
        Button setHeartBeatButton = new Button("Set Heartbeat");

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
        Button clearButton = new Button("Clear Terminal");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(400);

        HBox row1 = new HBox(10);
        row1.setPadding(new Insets(10));
        row1.getChildren().addAll(
                new Label("Heartbeat Checking:"),
                heartBeatCheckingField,
                new Label("Heartbeat Making:"),
                heartBeatMakingField,
                setHeartBeatButton
        );

        HBox row2 = new HBox(10);
        row2.setPadding(new Insets(10));
        row2.getChildren().addAll(
                new Label("Speed:"),
                serverSpeedsField,
                new Label("Queue Capacity:"),
                serverQueueCapsField,
                new Label("No. of servers:"),
                noOfServersField,
                submitManyButton
        );

        HBox row3 = new HBox(10);
        row3.setPadding(new Insets(10));
        row3.getChildren().addAll(
                new Label("Speed:"),
                serverSpeedField,
                new Label("Queue Capacity:"),
                serverQueueCapacityField,
                submitOneButton
        );

        HBox row4 = new HBox(10);
        row4.setPadding(new Insets(10));
        row4.getChildren().addAll(
                exitButton,
                clearButton
        );

        root.getChildren().addAll(row1, row2, row3, row4, logArea);

        setHeartBeatButton.setOnAction(e -> {
            try {
                int heartBeatChecking = Integer.parseInt(heartBeatCheckingField.getText());
                int heartBeatMaking = Integer.parseInt(heartBeatMakingField.getText());

                if (heartBeatChecking <= heartBeatMaking) {
                    appendLog("Heartbeat Checking interval must be greater than Heartbeat Making interval");
                    return;
                }

                boolean successSetHeartBeat = serverService.setHeartBeatIntervals(
                        Map.of("checking", heartBeatChecking, "making", heartBeatMaking)
                );
                appendLog("Heartbeat intervals set: " + successSetHeartBeat);
            } catch (NumberFormatException ex) {
                appendLog("Invalid heartbeat input");
            }
        });

        submitOneButton.setOnAction(e -> {
            try {
                double serverSpeed = Double.parseDouble(serverSpeedField.getText());
                int serverQueueCapacity = Integer.parseInt(serverQueueCapacityField.getText());
                boolean success = serverService.setServersOneByOne(new SpeedAndCapObj(serverSpeed, serverQueueCapacity));
                appendLog("Server submitted: " + success + " with speed: " + serverSpeed + " and queue capacity: " + serverQueueCapacity);
            } catch (NumberFormatException ex) {
                appendLog("Invalid input");
            }
        });

        submitManyButton.setOnAction(e -> {
            try {
                int noOfServers = Integer.parseInt(noOfServersField.getText());
                double serversSpeed = Double.parseDouble(serverSpeedsField.getText());
                int serversQueueCap = Integer.parseInt(serverQueueCapsField.getText());
                boolean success = serverService.setServersMany(noOfServers, new SpeedAndCapObj(serversSpeed, serversQueueCap));
                appendLog(noOfServers + " servers submitted: " + success);
            } catch (NumberFormatException ex) {
                appendLog("Invalid input in default fields");
            }
        });

        clearButton.setOnAction(e -> logArea.clear());
        exitButton.setOnAction(e -> Platform.exit());
    }

    private void appendLog(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    public Pane getRoot() {
        return root;
    }
}
