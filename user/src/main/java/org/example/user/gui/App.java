package org.example.user.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.user.service.Inputs;
import org.springframework.web.client.RestTemplate;

public class App extends Application {

    private String schedulingMode;
    private final Inputs inputs = new Inputs(new RestTemplate());

    @Override
    public void start(Stage primaryStage) {
        String messageBroker;
        Stage selectionStage = new Stage();
        VBox selectionRoot = new VBox(10);
        selectionRoot.setPadding(new Insets(15));

        Label selectionLabel = new Label("Select a Scheduling Model:");
        Button priorityButton = new Button("Priority-based Scheduling");
        Button fetchButton = new Button("Complete-and-then-Fetch");
        Button wlbButton = new Button("Weight-load-balancing");

        HBox selectionButtons = new HBox(10, priorityButton, fetchButton, wlbButton);

        Label brokerLabel = new Label("Select Message Broker:");
        ComboBox<String> brokerComboBox = new ComboBox<>(
                FXCollections.observableArrayList("Kafka", "RabbitMQ"));
        brokerComboBox.setValue("Kafka");

        brokerComboBox.setOnAction(e -> {
            String selectedBroker = brokerComboBox.getValue().toLowerCase();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Message Broker");
            alert.setHeaderText("Broker Set");
            alert.setContentText("Message Broker set to: " + selectedBroker);
            alert.showAndWait();
        });

        VBox brokerSelectionBox = new VBox(10, brokerLabel, brokerComboBox);
        selectionRoot.getChildren().addAll(selectionLabel, selectionButtons, brokerSelectionBox);


        Scene selectionScene = new Scene(selectionRoot, 300, 200);
        selectionStage.setTitle("Select Scheduling Model");
        selectionStage.setScene(selectionScene);
        selectionStage.show();


        priorityButton.setOnAction(e -> {
            schedulingMode = "age-based-priority-scheduling";
            selectionStage.close();

            boolean setModeSuccess1;
            String selectedBroker = brokerComboBox.getValue().toLowerCase();
            setModeSuccess1 = inputs.setAlgorithm(schedulingMode, selectedBroker);

            if(setModeSuccess1) {
                showMainStage(primaryStage, schedulingMode, selectedBroker);
            }
            else{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to Set Scheduling Model");
                alert.setContentText("The scheduling model could not be set. Please try again.");
                alert.showAndWait();

                start(new Stage());
            }
        });

        fetchButton.setOnAction(e -> {
            schedulingMode = "complete-and-then-fetch";
            selectionStage.close();

            boolean setModeSuccess1;
            boolean setModeSuccess2;
            boolean setModeSuccess3;
            String selectedBroker = brokerComboBox.getValue().toLowerCase();
            setModeSuccess1 = inputs.setAlgorithm(schedulingMode, selectedBroker);

            setModeSuccess3 = true;

            if(setModeSuccess1 && setModeSuccess3) {
                showMainStage(primaryStage, schedulingMode, selectedBroker);
            }
            else{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to Set Scheduling Model");
                alert.setContentText("The scheduling model could not be set. Please try again.");
                alert.showAndWait();

                start(new Stage());
            }
        });

        wlbButton.setOnAction(e -> {
            schedulingMode = "weight-load-balancing";
            selectionStage.close();

            boolean setModeSuccess1;
            boolean setModeSuccess3;
            String selectedBroker = brokerComboBox.getValue().toLowerCase();
            setModeSuccess1 = inputs.setAlgorithm(schedulingMode, selectedBroker);

            setModeSuccess3 = true;

            if(setModeSuccess1 && setModeSuccess3) {
                showMainStage(primaryStage, schedulingMode, selectedBroker);
            }
            else{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to Set Scheduling Model");
                alert.setContentText("The scheduling model could not be set. Please try again.");
                alert.showAndWait();

                start(new Stage());
            }
        });
    }

    private void showMainStage(Stage primaryStage, String mode, String selectedBroker) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label label = new Label("Message Broker: " + selectedBroker);
        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");
        Button clearButton = new Button("Clear");
        Button backButton = new Button("Back to Home");
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);

        HBox buttons = new HBox(10, yesButton, noButton, clearButton, backButton);

        root.getChildren().addAll(label, buttons, outputArea);

        // Button actions
        yesButton.setOnAction(e -> {
            inputs.runTimedHelloWorld(outputArea);
        });

        noButton.setOnAction(e -> {
            outputArea.appendText("Program terminated.\n");
        });

        clearButton.setOnAction(e -> {
            outputArea.clear();
        });

        backButton.setOnAction(e -> {
            primaryStage.close();
            start(new Stage());
        });

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle(mode + " Model");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
