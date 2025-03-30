package com.jobportal.controllers;

import com.jobportal.App;
import com.jobportal.models.User;
import com.jobportal.services.AuthService;
import com.jobportal.services.EmailService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {
    
    @FXML
    private VBox emailStep;
    
    @FXML
    private VBox verificationStep;
    
    @FXML
    private VBox resetPasswordStep;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField verificationField;
    
    @FXML
    private PasswordField newPasswordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Text errorText;
    
    @FXML
    private Text successText;
    
    private AuthService authService = new AuthService();
    private EmailService emailService = new EmailService();
    
    private String userEmail;
    private String verificationCode;
    private User userToReset;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Show email step initially
        showEmailStep();
    }
    
    private void showEmailStep() {
        emailStep.setVisible(true);
        verificationStep.setVisible(false);
        resetPasswordStep.setVisible(false);
    }
    
    private void showVerificationStep() {
        emailStep.setVisible(false);
        verificationStep.setVisible(true);
        resetPasswordStep.setVisible(false);
    }
    
    private void showResetPasswordStep() {
        emailStep.setVisible(false);
        verificationStep.setVisible(false);
        resetPasswordStep.setVisible(true);
    }
    
    @FXML
    private void onSendVerificationCodeClick() {
        userEmail = emailField.getText().trim();
        
        if (userEmail.isEmpty()) {
            showError("Please enter your email address");
            return;
        }
        
        // Check if email exists in the system
        // Change this line to use isEmailTaken instead of emailExists
        if (!authService.isEmailTaken(userEmail)) {
            showError("No account found with this email address");
            return;
        }
        
        try {
            // Get the user object for later use
            userToReset = authService.findUserByEmail(userEmail);
            
            // Generate and send verification code
            verificationCode = emailService.sendPasswordResetCode(userEmail);
            
            // Show verification step
            showVerificationStep();
            
            // Clear error message
            clearMessages();
            
        } catch (Exception e) {
            showError("Failed to send verification code: " + e.getMessage());
        }
    }
    
    @FXML
    private void onVerifyCodeClick() {
        String code = verificationField.getText().trim();
        
        if (code.isEmpty()) {
            showError("Please enter the verification code");
            return;
        }
        
        if (!code.equals(verificationCode)) {
            showError("Invalid verification code");
            return;
        }
        
        // Code is valid, proceed to reset password
        showResetPasswordStep();
        
        // Clear error message
        clearMessages();
    }
    
    @FXML
    private void onResetPasswordClick() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please enter and confirm your new password");
            return;
        }
        
        if (newPassword.length() < 6) {
            showError("Password must be at least 6 characters long");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        try {
            // Reset the password using ObjectId, not String
            boolean success = authService.resetPassword(userToReset.getId(), newPassword);
            
            if (success) {
                // Show success message
                showSuccess("Your password has been reset successfully");
                
                // Clear verification code
                emailService.clearVerificationCode(userEmail);
                
                // Auto-redirect to login after a delay
                new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {
                            javafx.application.Platform.runLater(() -> {
                                try {
                                    App.setRoot("login");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }, 
                    3000 // 3 seconds delay
                );
            } else {
                showError("Failed to reset password. Please try again.");
            }
        } catch (Exception e) {
            showError("Error resetting password: " + e.getMessage());
        }
    }
    
    @FXML
    private void onBackToEmailClick() {
        showEmailStep();
    }
    
    @FXML
    private void onBackToLoginClick() {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            showError("Navigation error: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisible(true);
        successText.setVisible(false);
    }
    
    private void showSuccess(String message) {
        successText.setText(message);
        successText.setVisible(true);
        errorText.setVisible(false);
    }
    
    private void clearMessages() {
        errorText.setVisible(false);
        successText.setVisible(false);
    }
}
