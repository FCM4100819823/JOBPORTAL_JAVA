package com.jobportal.controllers;

import com.jobportal.App;
import com.jobportal.models.User;
import com.jobportal.services.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorMessage;
    
    private AuthService authService = new AuthService();
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }
        
        try {
            User user = authService.authenticateUser(username, password);
            if (user != null) {
                // Authentication successful, navigate to appropriate dashboard
                navigateToDashboard(user);
            } else {
                showError("Invalid username or password");
            }
        } catch (Exception e) {
            showError("Login error: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleSignUp() {
        try {
            App.setRoot("register");
        } catch (IOException e) {
            showError("Navigation error: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleForgotPassword() {
        try {
            App.setRoot("forgotPassword");
        } catch (IOException e) {
            showError("Navigation error: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorMessage.setText(message);
        errorMessage.setVisible(true);
        errorMessage.setManaged(true);
    }
    
    private void navigateToDashboard(User user) {
        try {
            switch (user.getRole()) {
                case JOB_SEEKER:
                    App.setRoot("jobseeker/dashboard");
                    break;
                case EMPLOYER:
                    App.setRoot("employer/dashboard");
                    break;
                case ADMIN:
                    App.setRoot("admin/dashboard");
                    break;
                default:
                    showError("Unknown user role");
            }
        } catch (IOException e) {
            showError("Navigation error: " + e.getMessage());
        }
    }
}
