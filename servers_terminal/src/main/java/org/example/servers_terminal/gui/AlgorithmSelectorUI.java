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
        algorithmComboBox.setValue("Complete and then fetch"); // default

        HBox algorithmBox = new HBox(10, algorithmLabel, algorithmComboBox);

        // 2) Priority CheckBox
        priorityCheckBox = new CheckBox("Based on priority");

        // 3) The Expiry Age input area (for Priority -> Expiry Age in ms)
        expiryAgeVBox = new VBox(5);
        expiryAgeVBox.setPadding(new Insets(10, 0, 0, 0));

        // By default, hide both the expiryAgeVBox and the addRowButton
        expiryAgeVBox.setVisible(false);
        expiryAgeVBox.setManaged(false);

        // Create two initial rows (minimum requirement)
        expiryAgeVBox.getChildren().addAll(
                createExpiryAgeRow(),
                createExpiryAgeRow()
        );

        // 3.a) "Add Row" button
        addRowButton = new Button("Add Row");
        // Hide the "Add Row" button initially
        addRowButton.setVisible(false);
        addRowButton.setManaged(false);

        addRowButton.setOnAction(e -> expiryAgeVBox.getChildren().add(createExpiryAgeRow()));

        // Put the rows + "Add Row" button into a separate container
        VBox priorityBox = new VBox(5, expiryAgeVBox, addRowButton);

        // Show/hide based on priorityCheckBox
        priorityCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            expiryAgeVBox.setVisible(newVal);
            expiryAgeVBox.setManaged(newVal);
            addRowButton.setVisible(newVal);
            addRowButton.setManaged(newVal);
        });

        // 4) "Set" button
        setButton = new Button("Set");
        setButton.setOnAction(e -> handleSetButton());

        // Add everything to the root layout
        rootLayout.getChildren().addAll(
                algorithmBox,
                priorityCheckBox,
                priorityBox,
                setButton
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

    /**
     * Called when the "Set" button is clicked.
     */
    private void handleSetButton() {
        String selectedAlgorithm = algorithmComboBox.getValue();
        boolean isPriorityBased = priorityCheckBox.isSelected();

        // Gather data if priority-based
        LinkedHashMap<Integer, Double> expiryAgeMap = new LinkedHashMap<>();
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
                            double expiryMs = Double.parseDouble(fields.get(1).getText().trim());
                            // Insert into map
                            expiryAgeMap.put(priority, expiryMs);
                        } catch (NumberFormatException ex) {
                            showAlert(Alert.AlertType.ERROR,
                                    "Invalid Input",
                                    "Please enter valid numeric values for priority and expiry age.");
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

        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Success", "Algorithm set successfully!");
        } else {
            showAlert(Alert.AlertType.ERROR, "Failure", "Failed to set the algorithm.");
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public Parent getRoot() {
        return rootLayout;
    }
}
