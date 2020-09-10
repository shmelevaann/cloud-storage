package ru.geekbrains.chiffa.cloudstorage;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(ClassLoader.getSystemResource("client.fxml"));
        primaryStage.setTitle("Cloud storage");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}
