package org.example.user.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.example.user.config.AppConfig;
import org.example.user.service.Inputs;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestTemplate;

public class App extends Application {

    private String schedulingMode;
    private Inputs inputs;

    @Override
    public void init() {
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        inputs = context.getBean(Inputs.class);
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);

        ComboBox<String> messageBrokerComboBox = new ComboBox<>(FXCollections.observableArrayList("kafka", "rabbitmq"));
        messageBrokerComboBox.setValue("kafka");

        Button startButton = new Button("Start");

        Button stopButton = new Button("Stop");
        stopButton.setDisable(true);

        Button clearButton = new Button("Clear Terminal");

        startButton.setOnAction(event -> {
            String selectedBroker = messageBrokerComboBox.getValue();
            inputs.setMessageBroker(selectedBroker);
            inputs.runTimedHelloWorld(outputArea);

            stopButton.setDisable(false);
        });

        stopButton.setOnAction(event -> {
            if (inputs != null) {
                inputs.runTimedHelloWorld(outputArea);
            }

            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        clearButton.setOnAction(event -> outputArea.clear());

        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        controls.getChildren().addAll(new Label("Message Broker:"), messageBrokerComboBox, startButton, stopButton, clearButton);

        root.getChildren().addAll(controls, outputArea);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Inputs GUI");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
