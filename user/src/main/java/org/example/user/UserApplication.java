package org.example.user;

import javafx.application.Application;
//import org.example.user.gui.App;
import org.example.user.gui.App;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserApplication {

    public static void main(String[] args) {
//        SpringApplication.run(UserApplication.class, args);
//        SpringApplication.run(UserApplication.class, args);
        Application.launch(App.class, args);
    }



}
