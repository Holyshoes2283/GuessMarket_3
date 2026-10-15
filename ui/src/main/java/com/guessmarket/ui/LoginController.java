package com.guessmarket.ui;

import com.guessmarket.ui.network.HttpClientAdapter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;


public class LoginController {

    @FXML private TextField usernameField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            errorLabel.setText("Username cannot be empty.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setText("Connecting to engine...");
        errorLabel.setTextFill(javafx.scene.paint.Color.GRAY);

        HttpClientAdapter.getInstance().login(username)
                .thenAccept(responsePayload -> Platform.runLater(() -> {
                    transitionToMainDashboard(username);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        loginButton.setDisable(false);
                        errorLabel.setTextFill(javafx.scene.paint.Color.web("#d32f2f"));

                        String errorMsg = ex.getMessage();
                        
                        if (errorMsg != null && errorMsg.contains("Body:")) {
                            String jsonStr = errorMsg.substring(errorMsg.indexOf("Body:") + 5).trim();
                            try {
                                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(jsonStr).getAsJsonObject();
                                if (json.has("error")) {
                                    errorMsg = json.get("error").getAsString();
                                } else {
                                    errorMsg = jsonStr;
                                }
                            } catch (Exception parseEx) {
                                errorMsg = jsonStr;
                            }
                        }
                        errorLabel.setText("Login Failed: " + errorMsg);
                    });
                    return null;
                });
    }

    private void transitionToMainDashboard(String authenticatedUsername) {
        try {
            
            URL resourceUrl = getClass().getResource("/Main.fxml");
            if (resourceUrl == null) {
                throw new IllegalStateException("CRITICAL ERROR: Cannot find /Main.fxml in src/main/resources/");
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent mainRoot = loader.load();

            
            
            MainController mainController = loader.getController();
            mainController.initData(authenticatedUsername);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene mainScene = new Scene(mainRoot, 1200, 800);

            stage.setTitle("GuessMarket - Trading Dashboard (" + authenticatedUsername + ")");
            stage.setScene(mainScene);
            stage.setResizable(true);
            stage.centerOnScreen();

        } catch (IOException e) {
            errorLabel.setText("Critical Error: Failed to load the main dashboard UI.");
            e.printStackTrace();
        }
    }
}