package org.example.servers_terminal.gui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

public class AlgorithmSelectorUI {
    private final RestTemplate restTemplate;
    @Getter
    private final VBox root;

    private ComboBox<String> algorithmComboBox;
    private CheckBox priorityCheckBox;
    private VBox expiryAgeVBox;
    private Button addRowButton;

    private TextField waitTimeField;

    private TextArea outputArea;

    public AlgorithmSelectorUI(ApplicationContext context) {
        this.restTemplate = context.getBean(RestTemplate.class);
        root = new VBox(10);
        root.setPadding(new Insets(15));
        createUI();
    }

    private void createUI() {
        Label algorithmLabel = new Label("Select Algorithm:");
        algorithmComboBox = new ComboBox<>();
        algorithmComboBox.getItems().addAll("Complete and then fetch", "Load balancing");
        algorithmComboBox.setValue("Complete and then fetch");

        HBox algorithmBox = new HBox(10, algorithmLabel, algorithmComboBox);

        priorityCheckBox = new CheckBox("Based on priority");

        expiryAgeVBox = new VBox(5);
        expiryAgeVBox.setPadding(new Insets(10, 0, 0, 0));
        expiryAgeVBox.setVisible(false);
        expiryAgeVBox.setManaged(false);
        expiryAgeVBox.getChildren().addAll(createExpiryAgeRow(), createExpiryAgeRow());

        addRowButton = new Button("Add Row");
        addRowButton.setVisible(false);
        addRowButton.setManaged(false);
        addRowButton.setOnAction(e -> expiryAgeVBox.getChildren().add(createExpiryAgeRow()));

        VBox priorityBox = new VBox(5, expiryAgeVBox, addRowButton);
        priorityCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            expiryAgeVBox.setVisible(newVal);
            expiryAgeVBox.setManaged(newVal);
            addRowButton.setVisible(newVal);
            addRowButton.setManaged(newVal);
        });

        Button setButton = new Button("Set");
        setButton.setOnAction(e -> handleSetButton());

        Label waitTimeLabel = new Label("Wait Time (ms):");
        waitTimeField = new TextField();
        waitTimeField.setPromptText("Enter wait time in ms");

        Button setWaitTimeButton = new Button("Edit Wait Time");
        setWaitTimeButton.setOnAction(e -> handleSetWaitTime());
        HBox waitTimeBox = new HBox(10, waitTimeLabel, waitTimeField, setWaitTimeButton);

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefHeight(200);

        Button clearTerminalButton = new Button("Clear Terminal");
        clearTerminalButton.setOnAction(e -> outputArea.clear());
        HBox setButtonBox = new HBox(10, setButton, clearTerminalButton);

        root.getChildren().addAll(
                algorithmBox,
                priorityCheckBox,
                priorityBox,
//                waitTimeBox,
                setButtonBox,
                outputArea
        );
    }

    private HBox createExpiryAgeRow() {
        Label priorityLabel = new Label("Priority:");
        TextField priorityField = new TextField();
        priorityField.setPromptText("e.g. 1");

        Label expiryLabel = new Label("Expiry (ms):");
        TextField expiryField = new TextField();
        expiryField.setPromptText("e.g. 5000");

        return new HBox(10, priorityLabel, priorityField, expiryLabel, expiryField);
    }

    private void handleSetButton() {
        String selectedAlgorithm = algorithmComboBox.getValue();
        boolean isPriorityBased = priorityCheckBox.isSelected();

        LinkedHashMap<Integer, Long> expiryAgeMap = new LinkedHashMap<>();
        if (isPriorityBased) {
            for (int i = 0; i < expiryAgeVBox.getChildren().size(); i++) {
                if (expiryAgeVBox.getChildren().get(i) instanceof HBox row) {
                    List<TextField> fields = new ArrayList<>();
                    for (var node : row.getChildren()) {
                        if (node instanceof TextField tf) {
                            fields.add(tf);
                        }
                    }
                    if (fields.size() == 2) {
                        try {
                            int priority = Integer.parseInt(fields.get(0).getText().trim());
                            long expiryMs = Long.parseLong(fields.get(1).getText().trim());
                            expiryAgeMap.put(priority, expiryMs);
                        } catch (NumberFormatException ex) {
                            outputArea.appendText("ERROR: Please enter valid numeric values for priority and expiry age.\n");
                            return;
                        }
                    }
                }
            }
        }

        boolean success = false;
        if ("Complete and then fetch".equals(selectedAlgorithm) && !isPriorityBased) {
            success = postRequest("set-complete-and-fetch", null);
        } else if ("Load balancing".equals(selectedAlgorithm) && !isPriorityBased) {
            success = postRequest("set-load-balancing", null);
        } else if ("Complete and then fetch".equals(selectedAlgorithm) && isPriorityBased) {
            success = postRequest("set-priority-complete-fetch", expiryAgeMap);
        } else if ("Load balancing".equals(selectedAlgorithm) && isPriorityBased) {
            success = postRequest("set-priority-load-balancing", expiryAgeMap);
        }

        if (success) {
            if (isPriorityBased) {
                outputArea.appendText(String.format("INFO: Algorithm '%s' set successfully with priority => expiry mapping: %s\n", selectedAlgorithm, expiryAgeMap));
            } else {
                outputArea.appendText(String.format("INFO: Algorithm '%s' set successfully.\n", selectedAlgorithm));
            }
        } else {
            outputArea.appendText(String.format("ERROR: Failed to set algorithm '%s'.\n", selectedAlgorithm));
        }
    }

    private void handleSetWaitTime() {
        String waitTimeText = waitTimeField.getText().trim();
        try {
            Long waitTime = Long.parseLong(waitTimeText);
            boolean success = postRequest("update-wait-time", waitTime);
            if (success) {
                outputArea.appendText("INFO: Wait time set to " + waitTime + " ms successfully.\n");
            } else {
                outputArea.appendText("ERROR: Failed to set wait time to " + waitTime + " ms.\n");
            }
        } catch (NumberFormatException ex) {
            outputArea.appendText("ERROR: Please enter a valid numeric wait time.\n");
        }
    }

    private <T> boolean postRequest(String suffix, T payload) {
        String url = "http://localhost:8083/consumer-one/" + suffix;
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
