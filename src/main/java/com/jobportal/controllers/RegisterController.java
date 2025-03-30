package com.jobportal.controllers;

import com.jobportal.App;
import com.jobportal.models.User;
import com.jobportal.services.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class RegisterController implements Initializable {
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private TextField fullNameField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private TextField locationField;
    
    @FXML
    private ComboBox<String> roleComboBox;
    
    @FXML
    private CheckBox termsCheckbox;
    
    @FXML
    private Button registerButton;
    
    @FXML
    private Text errorText;
    
    private final AuthService authService = new AuthService();
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize role combo box options
        roleComboBox.getItems().addAll("Job Seeker", "Employer");
        roleComboBox.getSelectionModel().select(0); // Default to Job Seeker
        
        // Initialize validation listeners
        setUpValidationListeners();
    }
    
    private void setUpValidationListeners() {
        // Real-time validation could be implemented here
        // For example, checking email format as user types
    }
    
    @FXML
    private void onRegisterButtonClick() {
        if (!validateForm()) {
            return;
        }
        
        // Create a new user object
        User user = new User();
        user.setUsername(usernameField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setFullName(fullNameField.getText().trim());
        user.setPhone(phoneField.getText().trim());
        user.setLocation(locationField.getText().trim());
        
        // Set role based on selection
        if (roleComboBox.getValue().equals("Job Seeker")) {
            user.setRole(User.Role.JOB_SEEKER);
        } else {
            user.setRole(User.Role.EMPLOYER);
        }
        
        try {
            // Change this line to use registerUser instead of register
            User registeredUser = authService.registerUser(user, passwordField.getText());
            
            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registration Successful");
            alert.setHeaderText(null);
            alert.setContentText("Your account has been created successfully. You can now log in.");
            alert.showAndWait();
            
            // Redirect to login screen
            App.setRoot("login");
            
        } catch (Exception e) {
            showError("Registration error: " + e.getMessage());
        }
    }
    
    private boolean validateForm() {
        // Check required fields
        if (usernameField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty() ||
                passwordField.getText().isEmpty() ||
                confirmPasswordField.getText().isEmpty() ||
                fullNameField.getText().trim().isEmpty() ||
                roleComboBox.getValue() == null) {
            
            showError("Please fill in all required fields");
            return false;
        }
        
        // Validate email format
        if (!EMAIL_PATTERN.matcher(emailField.getText().trim()).matches()) {
            showError("Please enter a valid email address");
            return false;
        }
        
        // Check password length
        if (passwordField.getText().length() < 6) {
            showError("Password must be at least 6 characters long");
            return false;
        }
        
        // Check if passwords match
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showError("Passwords do not match");
            return false;
        }
        
        // Check terms agreement
        if (!termsCheckbox.isSelected()) {
            showError("You must agree to the Terms of Service and Privacy Policy");
            return false;
        }
        
        return true;
    }
    
    @FXML
    private void onSignInLinkClick() {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            showError("Navigation error: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisible(true);
    }
}
