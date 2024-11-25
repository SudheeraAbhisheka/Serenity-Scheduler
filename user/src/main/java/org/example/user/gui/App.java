//package org.example.user.gui;
//
//import javafx.application.Application;
//import javafx.geometry.Insets;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.layout.*;
//import javafx.stage.Stage;
//import org.example.user.service.Inputs;
//import org.springframework.web.client.RestTemplate;
//
//public class App extends Application {
//
//    @Override
//    public void start(Stage primaryStage) {
//        Inputs inputs = new Inputs(new RestTemplate());
//
//        // UI Elements
//        VBox root = new VBox(10);
//        root.setPadding(new Insets(15));
//
//        Label label = new Label("Do you want to run timedHelloWorld?");
//        Button yesButton = new Button("Yes");
//        Button noButton = new Button("No");
//        TextArea outputArea = new TextArea();
//        outputArea.setEditable(false);
//
//        HBox buttons = new HBox(10, yesButton, noButton);
//
//        root.getChildren().addAll(label, buttons, outputArea);
//
//        // Event Handlers
//        yesButton.setOnAction(e -> {
//            inputs.timedHelloWorld(outputArea);
//        });
//
//        noButton.setOnAction(e -> {
//            outputArea.appendText("Program terminated.\n");
//        });
//
//        // Scene setup
//        Scene scene = new Scene(root, 400, 300);
//        primaryStage.setTitle("Timed Hello World");
//        primaryStage.setScene(scene);
//        primaryStage.show();
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}
