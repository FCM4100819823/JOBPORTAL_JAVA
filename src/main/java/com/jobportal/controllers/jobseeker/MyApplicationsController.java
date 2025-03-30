package com.jobportal.controllers.jobseeker;

import com.jobportal.App;
import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.models.JobApplication;
import com.jobportal.services.ApplicationService;
import com.jobportal.services.JobService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.bson.types.ObjectId;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MyApplicationsController implements Initializable {
    
    @FXML
    private ComboBox<String> statusFilterComboBox;
    
    @FXML
    private ComboBox<String> sortComboBox;
    
    @FXML
    private VBox applicationsContainer;
    
    @FXML
    private StackPane loadingPane;
    
    @FXML
    private VBox emptyStatePane;
    
    private DashboardController parentController;
    private ApplicationService applicationService;
    private JobService jobService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applicationService = new ApplicationService();
        jobService = new JobService();
        
        // Initialize filter options
        initializeFilterOptions();
        
        // Initialize sort options
        initializeSortOptions();
    }
    
    private void initializeFilterOptions() {
        statusFilterComboBox.getItems().addAll(
                "All Applications",
                "Submitted",
                "Under Review",
                "Interview Scheduled",
                "Rejected",
                "Offer Extended",
                "Withdrawn"
        );
        
        statusFilterComboBox.getSelectionModel().select(0);
        
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            loadApplications();
        });
    }
    
    private void initializeSortOptions() {
        sortComboBox.getItems().addAll(
                "Latest First",
                "Oldest First"
        );
        
        sortComboBox.getSelectionModel().select(0);
        
        sortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            loadApplications();
        });
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        loadApplications();
    }
    
    private void loadApplications() {
        if (parentController == null || parentController.currentUser == null) {
            return;
        }
        
        // Show loading
        loadingPane.setVisible(true);
        loadingPane.setManaged(true);
        emptyStatePane.setVisible(false);
        emptyStatePane.setManaged(false);
        
        // Clear existing applications
        applicationsContainer.getChildren().removeIf(node -> node.getStyleClass().contains("job-card"));
        
        // Get applications in background
        Task<List<JobApplication>> task = new Task<>() {
            @Override
            protected List<JobApplication> call() {
                ObjectId userId = parentController.currentUser.getId();
                
                String selectedFilter = statusFilterComboBox.getValue();
                if (selectedFilter != null && !selectedFilter.equals("All Applications")) {
                    JobApplication.Status status = convertStringToStatus(selectedFilter);
                    return applicationService.getApplicationsByStatus(userId, status);
                } else {
                    return applicationService.getApplicationsByApplicant(userId);
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            List<JobApplication> applications = task.getValue();
            displayApplications(applications);
        });
        
        task.setOnFailed(event -> {
            // Hide loading
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            
            // Show error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to Load Applications");
            alert.setContentText("An error occurred while loading your applications. Please try again.");
            alert.showAndWait();
        });
        
        new Thread(task).start();
    }
    
    private void displayApplications(List<JobApplication> applications) {
        Platform.runLater(() -> {
            // Hide loading
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            
            if (applications.isEmpty()) {
                // Show empty state
                emptyStatePane.setVisible(true);
                emptyStatePane.setManaged(true);
                return;
            }
            
            // Sort applications if needed
            sortApplications(applications);
            
            // Create application cards
            for (JobApplication application : applications) {
                try {
                    Node applicationCard = createApplicationCard(application);
                    applicationsContainer.getChildren().add(applicationCard);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void sortApplications(List<JobApplication> applications) {
        String sortOption = sortComboBox.getValue();
        if (sortOption == null) return;
        
        if (sortOption.equals("Oldest First")) {
            applications.sort((a1, a2) -> a1.getSubmissionDate().compareTo(a2.getSubmissionDate()));
        } else {
            // Default: Latest First
            applications.sort((a1, a2) -> a2.getSubmissionDate().compareTo(a1.getSubmissionDate()));
        }
    }
    
    private Node createApplicationCard(JobApplication application) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/components/applicationCard.fxml"));
        Node card = loader.load();
        
        // Get job details
        Job job = jobService.getJobById(application.getJobId());
        
        // Set job info
        Text jobTitleText = (Text) card.lookup("#jobTitleText");
        Text companyText = (Text) card.lookup("#companyText");
        Text locationText = (Text) card.lookup("#locationText");
        
        if (job != null) {
            jobTitleText.setText(job.getTitle());
            companyText.setText(job.getCompany());
            locationText.setText(job.getLocation());
        } else {
            jobTitleText.setText("Unknown Job");
            companyText.setText("Unknown Company");
            locationText.setText("Unknown Location");
        }
        
        // Set application details
        Label statusLabel = (Label) card.lookup("#statusLabel");
        Text applicationDateText = (Text) card.lookup("#applicationDateText");
        Text lastUpdatedText = (Text) card.lookup("#lastUpdatedText");
        
        // Set status with appropriate style
        statusLabel.setText(application.getStatusDisplay());
        String statusStyleClass = "status-" + application.getStatus().toString().toLowerCase().replace("_", "-");
        statusLabel.getStyleClass().add(statusStyleClass);
        
        // Format dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        applicationDateText.setText(application.getSubmissionDate().format(formatter));
        lastUpdatedText.setText(application.getLastUpdateDate().format(formatter));
        
        // Set resume link
        Hyperlink viewResumeLink = (Hyperlink) card.lookup("#viewResumeLink");
        viewResumeLink.setOnAction(e -> openResume(application.getResumePath()));
        
        // Set buttons
        Button withdrawButton = (Button) card.lookup("#withdrawButton");
        Button viewJobButton = (Button) card.lookup("#viewJobButton");
        Button viewDetailsButton = (Button) card.lookup("#viewDetailsButton");
        
        // Disable withdraw button if already withdrawn or has an offer
        if (application.getStatus() == JobApplication.Status.WITHDRAWN ||
                application.getStatus() == JobApplication.Status.OFFER_ACCEPTED ||
                application.getStatus() == JobApplication.Status.OFFER_DECLINED) {
            withdrawButton.setDisable(true);
        }
        
        // Set button actions
        withdrawButton.setOnAction(e -> withdrawApplication(application));
        viewJobButton.setOnAction(e -> viewJob(application.getJobId().toString()));
        viewDetailsButton.setOnAction(e -> viewApplicationDetails(application.getId().toString()));
        
        return card;
    }
    
    private void openResume(String resumePath) {
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
    
    private void withdrawApplication(JobApplication application) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Withdrawal");
        confirmAlert.setHeaderText("Withdraw Application");
        confirmAlert.setContentText("Are you sure you want to withdraw your application for this position? This action cannot be undone.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = applicationService.withdrawApplication(application.getId());
                if (success) {
                    // Reload applications
                    loadApplications();
                    
                    // Show success message
                    showAlert(Alert.AlertType.INFORMATION, "Application Withdrawn", 
                            "Application Successfully Withdrawn", 
                            "Your application has been withdrawn successfully.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to Withdraw", 
                            "An error occurred while trying to withdraw your application. Please try again.");
                }
            }
        });
    }
    
    private void viewJob(String jobId) {
        // Navigate to job detail screen
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/jobDetail.fxml"));
            Node jobDetailView = loader.load();
            
            // Initialize controller
            JobDetailController controller = loader.getController();
            controller.setParentController(parentController);
            controller.loadJob(jobId, "applications");
            
            // Display job detail view
            parentController.contentArea.getChildren().clear();
            parentController.contentArea.getChildren().add(jobDetailView);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Navigation Error", 
                    "An error occurred while trying to view the job details.");
        }
    }
    
    private void viewApplicationDetails(String applicationId) {
        // In a real implementation, you would navigate to a detailed view
        // of the application with more information
        showAlert(Alert.AlertType.INFORMATION, "Application Details", 
                "Application ID: " + applicationId, 
                "Detailed application view is not implemented yet.");
    }
    
    @FXML
    private void onBrowseJobsClick() {
        // Navigate to browse jobs screen
        if (parentController instanceof JobSeekerDashboardController) {
            ((JobSeekerDashboardController) parentController).loadBrowseJobs();
        }
    }
    
    private JobApplication.Status convertStringToStatus(String statusStr) {
        return switch (statusStr) {
            case "Submitted" -> JobApplication.Status.SUBMITTED;
            case "Under Review" -> JobApplication.Status.UNDER_REVIEW;
            case "Interview Scheduled" -> JobApplication.Status.INTERVIEW_SCHEDULED;
            case "Rejected" -> JobApplication.Status.REJECTED;
            case "Offer Extended" -> JobApplication.Status.OFFER_EXTENDED;
            case "Withdrawn" -> JobApplication.Status.WITHDRAWN;
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
