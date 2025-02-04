package org.example.servers_terminal.gui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.servers_terminal.service.Server1Service;
import org.springframework.context.ApplicationContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

public class AlgorithmSelectorUI {

    private final ApplicationContext context;
    private final Server1Service server1Service;
    private final VBox rootLayout;

    // UI controls
    private ComboBox<String> algorithmComboBox;
    private CheckBox priorityCheckBox;
    private VBox expiryAgeVBox;   // Will hold rows of (priority, expiry) input
    private Button addRowButton;
    private Button setButton;

    // New UI controls for wait time input
    private TextField waitTimeField;
    private Button setWaitTimeButton;
    private Button clearTerminalButton;

    // TextArea for displaying messages (instead of Alerts)
    private TextArea outputArea;

    public AlgorithmSelectorUI(ApplicationContext context) {
        this.context = context;
        this.server1Service = context.getBean(Server1Service.class);

        // Root layout container
        rootLayout = new VBox(10);
        rootLayout.setPadding(new Insets(15));

        createUI();
    }

    private void createUI() {
        // 1) Algorithm ComboBox
        Label algorithmLabel = new Label("Select Algorithm:");
        algorithmComboBox = new ComboBox<>();
        algorithmComboBox.getItems().addAll("Complete and then fetch", "Load balancing");
        algorithmComboBox.setValue("Complete and then fetch");

        HBox algorithmBox = new HBox(10, algorithmLabel, algorithmComboBox);

        // 2) Priority CheckBox
        priorityCheckBox = new CheckBox("Based on priority");

        // 3) The Expiry Age input area
        expiryAgeVBox = new VBox(5);
        expiryAgeVBox.setPadding(new Insets(10, 0, 0, 0));
        expiryAgeVBox.setVisible(false);
        expiryAgeVBox.setManaged(false);
        expiryAgeVBox.getChildren().addAll(createExpiryAgeRow(), createExpiryAgeRow());

        // 3.a) "Add Row" button
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

        // 4) "Set" button
        setButton = new Button("Set");
        setButton.setOnAction(e -> handleSetButton());

        // 5) Wait Time input and button
        Label waitTimeLabel = new Label("Wait Time (ms):");
        waitTimeField = new TextField();
        waitTimeField.setPromptText("Enter wait time in ms");
        setWaitTimeButton = new Button("Set Wait Time");
        setWaitTimeButton.setOnAction(e -> handleSetWaitTime());
        HBox waitTimeBox = new HBox(10, waitTimeLabel, waitTimeField, setWaitTimeButton);

        // 6) TextArea for output
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefHeight(200);

        // 7) "Clear Terminal" button
        clearTerminalButton = new Button("Clear Terminal");
        clearTerminalButton.setOnAction(e -> outputArea.clear());
        HBox setButtonBox = new HBox(10, setButton, clearTerminalButton);

        // Add everything to the root layout
        rootLayout.getChildren().addAll(
                algorithmBox,
                priorityCheckBox,
                priorityBox,
                waitTimeBox,
                setButtonBox,
                outputArea
        );
    }

    /**
     * Creates a single row (HBox) containing:
     *  - A TextField for Priority (integer)
     *  - A TextField for Expiry Age (double)
     */
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

        // Gather data if priority-based
        LinkedHashMap<Integer, Long> expiryAgeMap = new LinkedHashMap<>();
        if (isPriorityBased) {
            for (int i = 0; i < expiryAgeVBox.getChildren().size(); i++) {
                if (expiryAgeVBox.getChildren().get(i) instanceof HBox row) {
                    // Extract text fields
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
                            // Insert into map
                            expiryAgeMap.put(priority, expiryMs);
                        } catch (NumberFormatException ex) {
                            outputArea.appendText("ERROR: Please enter valid numeric values "
                                    + "for priority and expiry age.\n");
                            return;
                        }
                    }
                }
            }
        }

        boolean success = false;
        // Decide which method to call on server1Service
        if ("Complete and then fetch".equals(selectedAlgorithm) && !isPriorityBased) {
            success = server1Service.setCompleteAndFetch();
        } else if ("Load balancing".equals(selectedAlgorithm) && !isPriorityBased) {
            success = server1Service.setLoadBalancing();
        } else if ("Complete and then fetch".equals(selectedAlgorithm) && isPriorityBased) {
            success = server1Service.setPriorityCompleteFetch(expiryAgeMap);
        } else if ("Load balancing".equals(selectedAlgorithm) && isPriorityBased) {
            success = server1Service.setPriorityLoadBalancing(expiryAgeMap);
        }

        // Construct more meaningful messages for output
        if (success) {
            if (isPriorityBased) {
                outputArea.appendText(String.format(
                        "INFO: Algorithm '%s' set successfully with priority => expiry mapping: %s\n",
                        selectedAlgorithm, expiryAgeMap
                ));
            } else {
                outputArea.appendText(String.format(
                        "INFO: Algorithm '%s' set successfully.\n",
                        selectedAlgorithm
                ));
            }
        } else {
            outputArea.appendText(String.format(
                    "ERROR: Failed to set algorithm '%s'.\n",
                    selectedAlgorithm
            ));
        }
    }

    private void handleSetWaitTime() {
        String waitTimeText = waitTimeField.getText().trim();
        try {
            Long waitTime = Long.parseLong(waitTimeText);
            boolean success = server1Service.updateLoadBalancingWaitTime(waitTime);
            if (success) {
                outputArea.appendText("INFO: Wait time set to " + waitTime + " ms successfully.\n");
            } else {
                outputArea.appendText("ERROR: Failed to set wait time to " + waitTime + " ms.\n");
            }
        } catch (NumberFormatException ex) {
            outputArea.appendText("ERROR: Please enter a valid numeric wait time.\n");
        }
    }

    public Parent getRoot() {
        return rootLayout;
    }
}
