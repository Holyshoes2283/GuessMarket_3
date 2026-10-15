package com.guessmarket.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;


public class GuessMarketClient extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        URL resourceUrl = getClass().getResource("/Login.fxml");
        if (resourceUrl == null) {
            throw new IllegalStateException("CRITICAL ERROR: Cannot find /Login.fxml in src/main/resources/");
        }

        FXMLLoader loader = new FXMLLoader(resourceUrl);
        Parent root = loader.load();

        Scene scene = new Scene(root, 400, 300);

        primaryStage.setTitle("GuessMarket - User Login");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}