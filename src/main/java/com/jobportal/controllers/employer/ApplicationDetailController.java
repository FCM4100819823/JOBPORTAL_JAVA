package com.jobportal.controllers.employer;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.models.JobApplication;
import com.jobportal.services.ApplicationService;
import com.jobportal.services.JobService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.bson.types.ObjectId;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ApplicationDetailController implements Initializable {
    
    @FXML
    private Text applicantNameText;
    
    @FXML
    private Text jobTitleText;
    
    @FXML
    private Text submissionDateText;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Text emailText;
    
    @FXML
    private Text phoneText;
    
    @FXML
    private Text locationText;
    
    @FXML
    private Hyperlink viewResumeLink;
    
    @FXML
    private TextFlow coverLetterTextFlow;
    
    @FXML
    private VBox additionalAnswersContainer;
    
    @FXML
    private TextArea notesTextArea;
    
    @FXML
    private ComboBox<String> statusComboBox;
    
    @FXML
    private Button updateStatusButton;
    
    private DashboardController parentController;
    private ApplicationsController applicationsController;
    private ApplicationService applicationService;
    private JobService jobService;
    
    private JobApplication currentApplication;
    private Job currentJob;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applicationService = new ApplicationService();
        jobService = new JobService();
        
        // Initialize status options
        initializeStatusOptions();
    }
    
    private void initializeStatusOptions() {
        statusComboBox.getItems().addAll(
                "Submitted",
                "Under Review",
                "Interview Scheduled",
                "Offer Extended",
                "Rejected"
        );
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
    }
    
    public void setApplicationsController(ApplicationsController controller) {
        this.applicationsController = controller;
    }
    
    public void loadApplication(ObjectId applicationId) {
        // Load application details
        currentApplication = applicationService.getApplicationById(applicationId);
        
        if (currentApplication != null) {
            // Load related job
            currentJob = jobService.getJobById(currentApplication.getJobId());
            
            // Display application details
            displayApplicationDetails();
        }
    }
    
    private void displayApplicationDetails() {
        // Set applicant info
        applicantNameText.setText(currentApplication.getFullName());
        
        // Set job info
        String jobTitle = currentJob != null ? currentJob.getTitle() : "Unknown Job";
        jobTitleText.setText(jobTitle);
        
        // Format submission date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        submissionDateText.setText("Applied on " + currentApplication.getSubmissionDate().format(formatter));
        
        // Set status with appropriate styling
        statusLabel.setText(currentApplication.getStatusDisplay());
        String statusStyleClass = "status-" + currentApplication.getStatus().toString().toLowerCase().replace("_", "-");
        statusLabel.getStyleClass().add(statusStyleClass);
        
        // Select current status in dropdown
        statusComboBox.setValue(getStatusString(currentApplication.getStatus()));
        
        // Set contact info
        emailText.setText(currentApplication.getEmail());
        phoneText.setText(currentApplication.getPhone());
        locationText.setText(currentApplication.getLocation());
        
        // Set resume link action
        viewResumeLink.setOnAction(e -> onViewResumeClick());
        
        // Set cover letter
        Text coverLetterText = new Text(currentApplication.getCoverLetter());
        coverLetterTextFlow.getChildren().clear();
        coverLetterTextFlow.getChildren().add(coverLetterText);
        
        // Set employer notes
        if (currentApplication.getEmployerNotes() != null) {
            notesTextArea.setText(currentApplication.getEmployerNotes());
        }
        
        // TODO: Display additional question answers if they exist
    }
    
    @FXML
    private void onViewResumeClick() {
        String resumePath = currentApplication.getResumePath();
        
        if (resumePath == null || resumePath.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Resume Not Found", 
                    "The resume file could not be found.");
            return;
        }
        
        try {
            File resumeFile = new File(resumePath);
            if (!resumeFile.exists()) {
                showAlert(Alert.AlertType.ERROR, "Error", "Resume Not Found", 
                        "The resume file could not be found at the specified location.");
                return;
            }
            
            // Open the file with the default system application
            java.awt.Desktop.getDesktop().open(resumeFile);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to Open Resume", 
                    "An error occurred while trying to open the resume file: " + e.getMessage());
        }
    }
    
    @FXML
    private void onSaveNotesClick() {
        // Get notes from text area
        String notes = notesTextArea.getText();
        
        // Update application with new notes
        boolean success = applicationService.updateEmployerNotes(currentApplication.getId(), notes);
        
        if (success) {
            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Success", "Notes Saved", 
                    "Your notes have been saved successfully.");
        } else {
            // Show error message
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to Save Notes", 
                    "An error occurred while saving your notes. Please try again.");
        }
    }
    
    @FXML
    private void onUpdateStatusClick() {
        String selectedStatus = statusComboBox.getValue();
        
        if (selectedStatus == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "No Status Selected", 
                    "Please select a status from the dropdown menu.");
            return;
        }
        
        // Convert string status to enum
        JobApplication.Status newStatus = convertToStatusEnum(selectedStatus);
        
        // Update application status
        boolean success = applicationService.updateApplicationStatus(currentApplication.getId(), newStatus);
        
        if (success) {
            // Reload application to show updated status
            loadApplication(currentApplication.getId());
            
            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Success", "Status Updated", 
                    "Application status has been updated successfully.");
        } else {
            // Show error message
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to Update Status", 
                    "An error occurred while updating the application status. Please try again.");
        }
    }
    
    @FXML
    private void onBackButtonClick() {
        // Go back to applications list
        if (applicationsController != null) {
            parentController.contentArea.getChildren().clear();
            ((EmployerDashboardController) parentController).loadApplications();
        }
    }
    
    private String getStatusString(JobApplication.Status status) {
        return switch (status) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under Review";
            case INTERVIEW_SCHEDULED -> "Interview Scheduled";
            case OFFER_EXTENDED -> "Offer Extended";
            case REJECTED -> "Rejected";
            case OFFER_ACCEPTED -> "Offer Accepted";
            case OFFER_DECLINED -> "Offer Declined";
            case WITHDRAWN -> "Withdrawn";
        };
    }
    
    private JobApplication.Status convertToStatusEnum(String statusStr) {
        return switch (statusStr) {
            case "Submitted" -> JobApplication.Status.SUBMITTED;
            case "Under Review" -> JobApplication.Status.UNDER_REVIEW;
            case "Interview Scheduled" -> JobApplication.Status.INTERVIEW_SCHEDULED;
            case "Offer Extended" -> JobApplication.Status.OFFER_EXTENDED;
            case "Rejected" -> JobApplication.Status.REJECTED;
            default -> JobApplication.Status.SUBMITTED;
        };
    }
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
