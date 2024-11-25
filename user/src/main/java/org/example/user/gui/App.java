package org.example.user.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.user.service.Inputs;
import org.springframework.web.client.RestTemplate;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        Inputs inputs = new Inputs(new RestTemplate());

        // UI Elements
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        Label label = new Label("Do you want to run timedHelloWorld?");
        Button yesButton = new Button("Yes");
        Button noButton = new Button("No");
        Button clearButton = new Button("Clear");
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);

        HBox buttons = new HBox(10, yesButton, noButton, clearButton);


        root.getChildren().addAll(label, buttons, outputArea);

        yesButton.setOnAction(e -> {
            inputs.runTimedHelloWorld(outputArea);
        });

        noButton.setOnAction(e -> {
            outputArea.appendText("Program terminated.\n");
        });

        clearButton.setOnAction(e -> {
            outputArea.clear();
        });


        // Scene setup
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("Timed Hello World");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
