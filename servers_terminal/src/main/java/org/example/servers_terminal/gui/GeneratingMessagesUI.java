package org.example.servers_terminal.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.servers_terminal.config.AppConfig;
import org.example.servers_terminal.service.MessageService;
import org.example.servers_terminal.service.Server1Service;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class GeneratingMessagesUI {
    private MessageService messageService;

    public GeneratingMessagesUI(ApplicationContext context) {
        this.messageService = context.getBean(MessageService.class);
    }

    public void show(Stage primaryStage) {
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
            messageService.setMessageBroker(selectedBroker);
            messageService.runTimedHelloWorld(outputArea);

            stopButton.setDisable(false);
        });

        stopButton.setOnAction(event -> {
            if (messageService != null) {
                messageService.runTimedHelloWorld(outputArea);
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
}
