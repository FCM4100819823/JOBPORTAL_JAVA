package com.jobportal.controllers.jobseeker;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.models.JobApplication;
import com.jobportal.services.ApplicationService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.bson.types.ObjectId;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.UUID;

public class JobApplicationController implements Initializable {
    
    @FXML
    private Text jobTitleText;
    
    @FXML
    private TextField fullNameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private TextField locationField;
    
    @FXML
    private Button uploadResumeButton;
    
    @FXML
    private Label resumeFileNameLabel;
    
    @FXML
    private TextArea coverLetterTextArea;
    
    @FXML
    private VBox additionalQuestionsContainer;
    
    @FXML
    private CheckBox consentCheckbox;
    
    @FXML
    private Text errorText;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private Button submitButton;
    
    private DashboardController parentController;
    private JobDetailController jobDetailController;
    private ApplicationService applicationService;
    private Job job;
    private File selectedResumeFile;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applicationService = new ApplicationService();
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
    }
    
    public void initializeForJob(Job job, JobDetailController jobDetailController) {
        this.job = job;
        this.jobDetailController = jobDetailController;
        
        // Set job title
        jobTitleText.setText(job.getTitle() + " at " + job.getCompany());
        
        // Pre-fill user info if available
        if (parentController != null && parentController.currentUser != null) {
            fullNameField.setText(parentController.currentUser.getFullName());
            emailField.setText(parentController.currentUser.getEmail());
            phoneField.setText(parentController.currentUser.getPhone());
            locationField.setText(parentController.currentUser.getLocation());
        }
        
        // Add any additional job-specific questions
        // This would typically come from the job posting data
        // For now, we'll just add a placeholder
        addAdditionalQuestion("What makes you a good fit for this position?");
    }
    
    private void addAdditionalQuestion(String questionText) {
        VBox questionBox = new VBox(5);
        Label questionLabel = new Label(questionText);
        TextArea answerArea = new TextArea();
        answerArea.setPromptText("Your answer...");
        answerArea.setPrefRowCount(3);
        answerArea.setWrapText(true);
        
        questionBox.getChildren().addAll(questionLabel, answerArea);
        additionalQuestionsContainer.getChildren().add(questionBox);
    }
    
    @FXML
    private void onUploadResumeClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Resume/CV");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Document Files", "*.pdf", "*.doc", "*.docx")
        );
        
        File file = fileChooser.showOpenDialog(uploadResumeButton.getScene().getWindow());
        if (file != null) {
            // Check file size (5MB limit)
            if (file.length() > 5 * 1024 * 1024) {
                showError("File size exceeds the 5MB limit. Please select a smaller file.");
                return;
            }
            
            selectedResumeFile = file;
            resumeFileNameLabel.setText(file.getName());
        }
    }
    
    @FXML
    private void onSubmitButtonClick() {
        if (!validateForm()) {
            return;
        }
        
        try {
            // Create application
            JobApplication application = new JobApplication();
            application.setJobId(job.getId());
            application.setApplicantId(parentController.currentUser.getId());
            application.setFullName(fullNameField.getText().trim());
            application.setEmail(emailField.getText().trim());
            application.setPhone(phoneField.getText().trim());
            application.setLocation(locationField.getText().trim());
            application.setCoverLetter(coverLetterTextArea.getText());
            
            // Upload resume if selected
            if (selectedResumeFile != null) {
                String resumePath = saveResumeFile(selectedResumeFile);
                application.setResumePath(resumePath);
            }
            
            // Submit application
            JobApplication submittedApplication = applicationService.submitApplication(application);
            
            // Show success message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Application Submitted");
            alert.setHeaderText(null);
            alert.setContentText("Your application has been submitted successfully!");
            alert.showAndWait();
            
            // Go back to job detail
            onBackButtonClick();
            
        } catch (IllegalStateException e) {
            // User has already applied
            showError(e.getMessage());
            
        } catch (Exception e) {
            showError("An error occurred while submitting your application. Please try again.");
            e.printStackTrace();
        }
    }
    
    @FXML
    private void onCancelButtonClick() {
        // Confirm cancel
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancel Application");
        alert.setHeaderText("Are you sure you want to cancel?");
        alert.setContentText("Your application information will not be saved.");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            onBackButtonClick();
        }
    }
    
    @FXML
    private void onBackButtonClick() {
        // Go back to job detail view and refresh
        if (jobDetailController != null) {
            jobDetailController.loadJob(job.getId().toString(), "browse");
            parentController.contentArea.getChildren().clear();
            parentController.contentArea.getChildren().add(jobDetailController.getRoot());
        } else {
            // Fallback
            ((JobSeekerDashboardController) parentController).loadBrowseJobs();
        }
    }
    
    private boolean validateForm() {
        // Clear previous error
        hideError();
        
        // Check required fields
        if (fullNameField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty() ||
                phoneField.getText().trim().isEmpty()) {
            showError("Please fill in all required fields");
            return false;
        }
        
        // Validate email format
        if (!emailField.getText().trim().matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            showError("Please enter a valid email address");
            return false;
        }
        
        // Check resume
        if (selectedResumeFile == null) {
            showError("Please upload your resume");
            return false;
        }
        
        // Check consent
        if (!consentCheckbox.isSelected()) {
            showError("You must agree to the privacy policy");
            return false;
        }
        
        return true;
    }
    
    private String saveResumeFile(File file) throws Exception {
        // Create directory for resumes if it doesn't exist
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "resumes");
        Files.createDirectories(uploadDir);
        
        // Generate unique filename
        String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getName();
        Path targetPath = uploadDir.resolve(uniqueFileName);
        
        // Copy file to target location
        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path
        return "uploads/resumes/" + uniqueFileName;
    }
    
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisible(true);
    }
    
    private void hideError() {
        errorText.setVisible(false);
    }
}
