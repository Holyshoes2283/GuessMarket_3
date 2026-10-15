package com.guessmarket.ui.controllers;


import com.guessmarket.engine.GuessMarketEngineFactory;
import com.guessmarket.engine.IGuessMarketEngine;
import com.guessmarket.engine.exceptions.InvalidDataLoadException;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;


public class DashboardController {

    @FXML private ProgressBar progressBar;
    @FXML private Button btnLoadXml;

    
    private IGuessMarketEngine engine;

    @FXML
    public void initialize() {
        
        this.engine = GuessMarketEngineFactory.createEngine();

        
        if (progressBar != null) {
            progressBar.setVisible(false);
            progressBar.setProgress(0);
        }
    }

    
    @FXML
    private void handleLoadXml() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Guess Market XML");

        
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extFilter);

        Stage stage = (Stage) btnLoadXml.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            processFileAsynchronously(file);
        }
    }

    
    private void processFileAsynchronously(File file) {
        
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0.1, 1.0);

                
                Thread.sleep(1500);

                updateProgress(0.5, 1.0);

                
                engine.getDataLoader().loadMarketData(file);

                updateProgress(1.0, 1.0);
                return null;
            }
        };

        
        if (progressBar != null) {
            progressBar.progressProperty().bind(loadTask.progressProperty());
            progressBar.setVisible(true);
        }

        if (btnLoadXml != null) {
            btnLoadXml.setDisable(true); 
        }

        
        loadTask.setOnSucceeded(event -> {
            cleanupTaskUI();

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Load Successful");
            success.setHeaderText("Market Data Ingested");
            success.setContentText("The XML file passed all Phase 8 validations and was committed.");
            success.showAndWait();
        });

        
        loadTask.setOnFailed(event -> {
            cleanupTaskUI();

            Throwable exception = loadTask.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Load Failed");
            alert.setHeaderText("Invalid Market Data");

            
            if (exception instanceof InvalidDataLoadException) {
                alert.setContentText(exception.getMessage());
            } else {
                alert.setContentText("An unexpected error occurred: " + exception.getMessage());
            }
            alert.showAndWait();
        });

        
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    
    private void cleanupTaskUI() {
        if (progressBar != null) {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            progressBar.setProgress(0);
        }
        if (btnLoadXml != null) {
            btnLoadXml.setDisable(false);
        }
    }
}