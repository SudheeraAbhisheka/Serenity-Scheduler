package org.example.servers_terminal.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.servers_terminal.config.AppConfig;
import org.example.servers_terminal.service.MessageService;
import org.example.servers_terminal.service.Server1Service;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class GeneratingMessagesUI {
    private MessageService messageService;
    private final Pane root;

    public GeneratingMessagesUI(ApplicationContext context) {
        this.messageService = context.getBean(MessageService.class);
        root = new VBox();
        show();
    }


    public void show() {
        root.setPadding(new Insets(10));

        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);

        ComboBox<String> messageBrokerComboBox = new ComboBox<>(FXCollections.observableArrayList("kafka", "rabbitmq"));
        messageBrokerComboBox.setValue("kafka");

        TextField noOfThreadsField = new TextField();
        noOfThreadsField.setPromptText("Enter No. of Threads");

        TextField noOfTasksField = new TextField();
        noOfTasksField.setPromptText("Enter No. of Tasks");

        Button startButton = new Button("Start");

        Button clearButton = new Button("Clear Terminal");

        startButton.setOnAction(event -> {
            String selectedBroker = messageBrokerComboBox.getValue();
            messageService.setMessageBroker(selectedBroker);

            try {
                int noOfThreads = Integer.parseInt(noOfThreadsField.getText());
                int noOfTasks = Integer.parseInt(noOfTasksField.getText());
                messageService.runTimedHelloWorld(outputArea, noOfThreads, noOfTasks);
            } catch (NumberFormatException e) {
                outputArea.appendText("Please enter valid numbers for threads and tasks.\n");
                return;
            }

        });

        clearButton.setOnAction(event -> outputArea.clear());

        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));

        controls.getChildren().addAll(
                new Label("Message Broker:"),
                messageBrokerComboBox,
                startButton,
                clearButton,
                new Label("Threads:"),
                noOfThreadsField,
                new Label("Tasks:"),
                noOfTasksField
        );

        root.getChildren().addAll(controls, outputArea);
    }

    public Pane getRoot() {
        return root;
    }
}
