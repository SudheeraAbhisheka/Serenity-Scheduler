package org.example.servers_terminal.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.example.servers_terminal.ServersTerminalApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApp extends Application {
    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(ServersTerminalApplication.class).run();
    }

    @Override
    public void start(Stage stage) {
        FxController controller = context.getBean(FxController.class);
        controller.start(stage);
    }

    @Override
    public void stop() {
        context.close();
        Platform.exit();
    }
}
