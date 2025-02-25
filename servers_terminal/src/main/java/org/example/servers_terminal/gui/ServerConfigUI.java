package org.example.servers_terminal.gui;

import com.example.SpeedAndCapObj;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class ServerConfigUI {
    private final RestTemplate restTemplate;
    private TextArea logArea;
    @Getter
    private final Pane root;

    public ServerConfigUI(ApplicationContext context) {
        this.restTemplate = context.getBean(RestTemplate.class);
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

        Button submitManyButton = new Button("Submit");
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
                exitButton,
                clearButton
        );

        root.getChildren().addAll(row1, row2, row3, logArea);

        setHeartBeatButton.setOnAction(e -> {
            try {
                int heartBeatChecking = Integer.parseInt(heartBeatCheckingField.getText());
                int heartBeatMaking = Integer.parseInt(heartBeatMakingField.getText());

                if (heartBeatChecking <= heartBeatMaking) {
                    appendLog("Heartbeat Checking interval must be greater than Heartbeat Making interval");
                    return;
                }

                boolean successSetHeartBeat = postRequest(
                        "set-heart-beat-intervals",
                        Map.of("checking", heartBeatChecking, "making", heartBeatMaking)
                );
                appendLog("Heartbeat intervals set: " + successSetHeartBeat);
            } catch (NumberFormatException ex) {
                appendLog("Invalid heartbeat input");
            }
        });

        submitManyButton.setOnAction(e -> {
            try {
                int noOfServers = Integer.parseInt(noOfServersField.getText());
                double serversSpeed = Double.parseDouble(serverSpeedsField.getText());
                int serversQueueCap = Integer.parseInt(serverQueueCapsField.getText());
                boolean success = postRequest(
                        "set-server-many?noOfQueues=" + noOfServers,
                        new SpeedAndCapObj(serversSpeed, serversQueueCap)
                );
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

    private <T> boolean postRequest(String suffix, T payload) {
        String url = "http://localhost:8084/api/" + suffix;
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
