package com.guessmarket.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainApp extends Application {

    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    
    @Override
    public void start(Stage primaryStage) {
        try {
            LOGGER.info("Bootstrapping Guess Market JavaFX Client...");

            
            
            URL fxmlLocation = getClass().getResource("/main.fxml");
            if (fxmlLocation == null) {
                throw new IOException("CRITICAL: Cannot locate /main.fxml. Ensure it is placed in the src/main/resources/ directory.");
            }

            
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            
            Object rawController = loader.getController();
            if (rawController == null) {
                throw new IllegalStateException("CRITICAL: main.fxml is missing an fx:controller definition in its root node.");
            }

            
            if (rawController instanceof MainController mainController) {

                
                mainController.initData("ClientUser");

                
                primaryStage.setOnCloseRequest(event -> mainController.shutdown());

            } else {
                throw new ClassCastException("CRITICAL: Controller specified in FXML is not of type MainController. Found: " + rawController.getClass().getName());
            }

            
            Scene scene = new Scene(root, 1024, 768);

            primaryStage.setTitle("GuessMarket - Trading Dashboard (Client Mode)");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();

            LOGGER.info("Guess Market UI successfully rendered and active.");

        } catch (Exception e) {
            
            
            
            System.err.println("=====================================================");
            System.err.println("UI BOOTSTRAP FAILED - FATAL ERROR");
            System.err.println("=====================================================");
            e.printStackTrace();

            LOGGER.log(Level.SEVERE, "FATAL ERROR: Could not bootstrap the application UI.", e);

            
            Platform.exit();
        }
    }

    
    @Override
    public void stop() throws Exception {
        LOGGER.info("Application termination signal received. Initiating client shutdown sequence...");
        super.stop();
        LOGGER.info("Client shutdown complete.");
    }

    
    public static void main(String[] args) {
        launch(args);
    }
}