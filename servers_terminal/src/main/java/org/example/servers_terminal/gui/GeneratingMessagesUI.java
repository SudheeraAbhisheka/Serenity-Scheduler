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

        // Existing fields
        TextField noOfThreadsField = new TextField();
        noOfThreadsField.setPromptText("Enter No. of Threads");

        TextField noOfTasksField = new TextField();
        noOfTasksField.setPromptText("Enter No. of Tasks");

        TextField minWeightField = new TextField("1");
        minWeightField.setPromptText("Enter Min Weight");

        TextField maxWeightField = new TextField("10");
        maxWeightField.setPromptText("Enter Max Weight");

        TextField minPriorityField = new TextField("1");
        minPriorityField.setPromptText("Enter Min Priority");

        TextField maxPriorityField = new TextField("10");
        maxPriorityField.setPromptText("Enter Max Priority");

        Button startButton = new Button("Start");
        Button clearButton = new Button("Clear Terminal");

        startButton.setOnAction(event -> {
            int noOfThreads = 0;
            int noOfTasks = 0;
            int minWeight = 0;
            int maxWeight = 0;
            int minPriority = 0;
            int maxPriority = 0;

            String selectedBroker = messageBrokerComboBox.getValue();
            messageService.setMessageBroker(selectedBroker);

            try {
                noOfThreads = Integer.parseInt(noOfThreadsField.getText());
                noOfTasks = Integer.parseInt(noOfTasksField.getText());
                minWeight = Integer.parseInt(minWeightField.getText());
                maxWeight = Integer.parseInt(maxWeightField.getText());
                minPriority = Integer.parseInt(minPriorityField.getText());
                maxPriority = Integer.parseInt(maxPriorityField.getText());
            } catch (NumberFormatException e) {
                outputArea.appendText("Please enter valid numbers for threads, tasks, weight, and priority.\n");
                return;
            }

            if (maxWeight < minWeight || maxPriority < minPriority) {
                outputArea.appendText("Please ensure max >= min for weight/priority.\n");
                return;
            }

            if(messageService.generateReport(noOfTasks)){
                outputArea.appendText("Report will generate at: \n");
            }
            else{
                outputArea.appendText("Error in generating report: \n");
                return;
            }

            messageService.runTimedHelloWorld(outputArea,
                    noOfThreads,
                    noOfTasks,
                    minWeight,
                    maxWeight,
                    minPriority,
                    maxPriority);
        });

        clearButton.setOnAction(event -> outputArea.clear());

        HBox row1 = new HBox(10);
        row1.setPadding(new Insets(10));
        row1.getChildren().addAll(
                new Label("Message Broker:"),
                messageBrokerComboBox,
                new Label("Threads:"),
                noOfThreadsField,
                new Label("Tasks:"),
                noOfTasksField
        );

        HBox row2 = new HBox(10);
        row2.setPadding(new Insets(10));
        row2.getChildren().addAll(
                new Label("Min W:"),
                minWeightField,
                new Label("Max W:"),
                maxWeightField,
                new Label("Min P:"),
                minPriorityField,
                new Label("Max P:"),
                maxPriorityField,
                startButton,
                clearButton
        );

        root.getChildren().addAll(row1, row2, outputArea);
    }


    public Pane getRoot() {
        return root;
    }
}
