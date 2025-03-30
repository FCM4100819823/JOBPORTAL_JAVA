package com.jobportal.controllers.admin;

import com.jobportal.models.User;
import com.jobportal.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class UserDialogController implements Initializable {
    
    @FXML
    private Text dialogTitle;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField fullNameField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private TextField locationField;
    
    @FXML
    private ComboBox<String> roleComboBox;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Text errorText;
    
    private User existingUser;
    private UserService userService;
    private boolean isEditMode;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        
        // Initialize role options
        roleComboBox.getItems().addAll(
                "Job Seeker", 
                "Employer", 
                "Admin"
        );
    }
    
    public void initializeDialog(User user) {
        isEditMode = (user != null);
        existingUser = user;
        
        // Update dialog title
        dialogTitle.setText(isEditMode ? "Edit User" : "Add New User");
        
        // Populate fields if editing
        if (isEditMode) {
            usernameField.setText(user.getUsername());
            emailField.setText(user.getEmail());
            fullNameField.setText(user.getFullName());
            phoneField.setText(user.getPhone());
            locationField.setText(user.getLocation());
            
            // Set role in dropdown
            if (user.getRole() != null) {
                switch (user.getRole()) {
                    case JOB_SEEKER -> roleComboBox.setValue("Job Seeker");
                    case EMPLOYER -> roleComboBox.setValue("Employer");
                    case ADMIN -> roleComboBox.setValue("Admin");
                }
            }
            
            // In edit mode, password is optional
            passwordField.setPromptText("Leave blank to keep current password");
            confirmPasswordField.setPromptText("Leave blank to keep current password");
            
            // Disable username in edit mode
            usernameField.setDisable(true);
        }
    }
    
    public User processResult() {
        if (!validateForm()) {
            return null;
        }
        
        try {
            // Create or update user
            User user = isEditMode ? existingUser : new User();
            
            // Set user properties
            if (!isEditMode) {
                user.setUsername(usernameField.getText().trim());
            }
            
            user.setEmail(emailField.getText().trim());
            user.setFullName(fullNameField.getText().trim());
            user.setPhone(phoneField.getText().trim());
            user.setLocation(locationField.getText().trim());
            
            // Set role
            String roleValue = roleComboBox.getValue();
            if (roleValue != null) {
                switch (roleValue) {
                    case "Job Seeker" -> user.setRole(User.Role.JOB_SEEKER);
                    case "Employer" -> user.setRole(User.Role.EMPLOYER);
                    case "Admin" -> user.setRole(User.Role.ADMIN);
                }
            }
            
            // Set password if provided
            String password = passwordField.getText();
            if (!password.isEmpty()) {
                user.setPasswordHash(userService.hashPassword(password));
            }
            
            // Save to database
            if (isEditMode) {
                userService.updateUser(user);
            } else {
                userService.createUser(user);
            }
            
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            setError("An error occurred: " + e.getMessage());
            return null;
        }
    }
    
    private boolean validateForm() {
        // Reset error
        clearError();
        
        // Validate required fields
        if (!isEditMode && (usernameField.getText().trim().isEmpty())) {
            setError("Username is required");
            return false;
        }
        
        if (emailField.getText().trim().isEmpty()) {
            setError("Email is required");
            return false;
        }
        
        if (fullNameField.getText().trim().isEmpty()) {
            setError("Full name is required");
            return false;
        }
        
        if (roleComboBox.getValue() == null) {
            setError("Role is required");
            return false;
        }
        
        // Validate username format
        if (!isEditMode && !usernameField.getText().trim().matches("^[a-zA-Z0-9_]{3,20}$")) {
            setError("Username must be 3-20 characters and can only contain letters, numbers, and underscores");
            return false;
        }
        
        // Validate email format
        if (!emailField.getText().trim().matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            setError("Please enter a valid email address");
            return false;
        }
        
        // Validate password (only if creating new user or changing password)
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (!isEditMode && password.isEmpty()) {
            setError("Password is required for new users");
            return false;
        }
        
        if (!password.isEmpty()) {
            // Password validation
            if (password.length() < 8) {
                setError("Password must be at least 8 characters");
                return false;
            }
            
            // Password confirmation
            if (!password.equals(confirmPassword)) {
                setError("Passwords do not match");
                return false;
            }
        }
        
        // Check if username is already taken (for new users)
        if (!isEditMode && userService.isUsernameTaken(usernameField.getText().trim())) {
            setError("Username is already taken");
            return false;
        }
        
        // Check if email is already taken (unless it's the same user)
        String email = emailField.getText().trim();
        if (userService.isEmailTaken(email) && (!isEditMode || !email.equals(existingUser.getEmail()))) {
            setError("Email is already registered");
            return false;
        }
        
        return true;
    }
    
    private void setError(String message) {
        errorText.setText(message);
        errorText.setVisible(true);
    }
    
    private void clearError() {
        errorText.setVisible(false);
    }
}
