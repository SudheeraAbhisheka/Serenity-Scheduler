package org.example.user.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.user.config.AppConfig;
import org.example.user.service.Inputs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

public class App extends Application {

    private String schedulingMode;
    private final Inputs inputs = new Inputs(new RestTemplate());

    @Override
    public void start(Stage primaryStage) {
        Stage selectionStage = new Stage();
        VBox selectionRoot = new VBox(10);
        selectionRoot.setPadding(new Insets(15));

        Label selectionLabel = new Label("Select a Scheduling Model:");
        Button priorityButton = new Button("Priority-based Scheduling");
        Button fetchButton = new Button("Complete-and-then-Fetch");

        HBox selectionButtons = new HBox(10, priorityButton, fetchButton);
        selectionRoot.getChildren().addAll(selectionLabel, selectionButtons);

        Scene selectionScene = new Scene(selectionRoot, 300, 200);
        selectionStage.setTitle("Select Scheduling Model");
        selectionStage.setScene(selectionScene);
        selectionStage.show();

        priorityButton.setOnAction(e -> {
            schedulingMode = "age-based-priority-scheduling";
            selectionStage.close();

            boolean setModeSuccess;
            setModeSuccess = inputs.setAlgorithm(schedulingMode);

            if(setModeSuccess) {
                showMainStage(primaryStage, schedulingMode);
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

            boolean setModeSuccess;
            setModeSuccess = inputs.setAlgorithm(schedulingMode);

            if(setModeSuccess) {
                showMainStage(primaryStage, schedulingMode);
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

    private void showMainStage(Stage primaryStage, String mode) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label label = new Label("Do you want to run timedHelloWorld?");
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
