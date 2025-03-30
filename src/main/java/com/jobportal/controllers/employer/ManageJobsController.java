package com.jobportal.controllers.employer;

import com.jobportal.App;
import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
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

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ManageJobsController implements Initializable {
    
    @FXML
    private ComboBox<String> statusFilterComboBox;
    
    @FXML
    private VBox jobsContainer;
    
    @FXML
    private StackPane loadingPane;
    
    @FXML
    private VBox emptyStatePane;
    
    private DashboardController parentController;
    private JobService jobService;
    private ApplicationService applicationService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobService = new JobService();
        applicationService = new ApplicationService();
        
        // Initialize filter options
        statusFilterComboBox.getItems().addAll(
                "All Jobs",
                "Active Jobs",
                "Inactive Jobs"
        );
        
        statusFilterComboBox.getSelectionModel().select(0);
        
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadJobs();
            }
        });
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        loadJobs();
    }
    
    private void loadJobs() {
        if (parentController == null || parentController.currentUser == null) {
            return;
        }
        
        // Show loading
        loadingPane.setVisible(true);
        loadingPane.setManaged(true);
        emptyStatePane.setVisible(false);
        emptyStatePane.setManaged(false);
        
        // Clear existing jobs
        jobsContainer.getChildren().removeIf(node -> node.getStyleClass().contains("job-card"));
        
        // Get jobs in background
        Task<List<Job>> task = new Task<>() {
            @Override
            protected List<Job> call() {
                ObjectId employerId = parentController.currentUser.getId();
                String filter = statusFilterComboBox.getValue();
                
                // Apply filters if necessary
                if ("Active Jobs".equals(filter)) {
                    return jobService.getActiveJobsByEmployer(employerId);
                } else if ("Inactive Jobs".equals(filter)) {
                    return jobService.getInactiveJobsByEmployer(employerId);
                } else {
                    return jobService.getJobsByEmployer(employerId);
                }
            }
        };
        
        task.setOnSucceeded(event -> {
            List<Job> jobs = task.getValue();
            displayJobs(jobs);
        });
        
        task.setOnFailed(event -> {
            // Hide loading
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            
            // Show error
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load jobs", 
                    "An error occurred while loading your jobs. Please try again.");
        });
        
        new Thread(task).start();
    }
    
    private void displayJobs(List<Job> jobs) {
        Platform.runLater(() -> {
            // Hide loading
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            
            if (jobs.isEmpty()) {
                // Show empty state
                emptyStatePane.setVisible(true);
                emptyStatePane.setManaged(true);
                return;
            }
            
            // Hide empty state
            emptyStatePane.setVisible(false);
            emptyStatePane.setManaged(false);
            
            // Create job cards for each job
            for (Job job : jobs) {
                try {
                    Node jobCard = createJobCard(job);
                    jobsContainer.getChildren().add(jobCard);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    private Node createJobCard(Job job) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/employer/components/jobManagementCard.fxml"));
        Node card = loader.load();
        
        // Set job info
        Text jobTitleText = (Text) card.lookup("#jobTitleText");
        Text companyText = (Text) card.lookup("#companyText");
        Text locationText = (Text) card.lookup("#locationText");
        Label statusLabel = (Label) card.lookup("#statusLabel");
        Text postedDateText = (Text) card.lookup("#postedDateText");
        Text deadlineText = (Text) card.lookup("#deadlineText");
        VBox deadlineContainer = (VBox) card.lookup("#deadlineContainer");
        Text viewsText = (Text) card.lookup("#viewsText");
        Text applicationsText = (Text) card.lookup("#applicationsText");
        
        jobTitleText.setText(job.getTitle());
        companyText.setText(job.getCompany());
        locationText.setText(job.getLocation());
        
        // Set status with appropriate styling
        boolean isActive = job.isActive();
        statusLabel.setText(job.getStatusDisplayText());
        statusLabel.getStyleClass().add("status-" + (isActive ? "active" : "inactive"));
        
        // Format dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        postedDateText.setText(job.getPostDate().format(formatter));
        
        // Set deadline if available
        if (job.getApplicationDeadline() != null) {
            deadlineText.setText(job.getApplicationDeadline().format(formatter));
            if (job.isExpired()) {
                deadlineText.getStyleClass().add("text-danger");
            }
        } else {
            deadlineContainer.setVisible(false);
            deadlineContainer.setManaged(false);
        }
        
        // Set stats
        viewsText.setText(String.valueOf(job.getViewCount()));
        
        // Get real application count
        int applicationCount = applicationService.getApplicationCount(job.getId());
        applicationsText.setText(String.valueOf(applicationCount));
        
        // Configure buttons
        ToggleButton activeToggle = (ToggleButton) card.lookup("#activeToggle");
        Button viewApplicationsButton = (Button) card.lookup("#viewApplicationsButton");
        Button editButton = (Button) card.lookup("#editButton");
        Button deleteButton = (Button) card.lookup("#deleteButton");
        
        // Set active toggle state and text
        activeToggle.setSelected(isActive);
        activeToggle.setText(isActive ? "Deactivate Job" : "Activate Job");
        
        // Set up button actions
        activeToggle.setOnAction(e -> toggleJobStatus(job, activeToggle.isSelected()));
        viewApplicationsButton.setOnAction(e -> viewApplications(job.getId()));
        editButton.setOnAction(e -> editJob(job.getId()));
        deleteButton.setOnAction(e -> deleteJob(job.getId()));
        
        return card;
    }
    
    private void toggleJobStatus(Job job, boolean active) {
        try {
            job.setActive(active);
            boolean success = jobService.updateJob(job);
            
            if (success) {
                // Reload jobs to reflect changes
                loadJobs();
            } else {
                // Show error
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to update job status", 
                        "An error occurred while trying to update the job status. Please try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update job status", 
                    "An error occurred: " + e.getMessage());
        }
    }
    
    private void viewApplications(ObjectId jobId) {
        // Navigate to the applications view for this job
        try {
            ((EmployerDashboardController) parentController).loadApplications();
            // TODO: Pass job ID to applications controller to filter
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Navigation Error", 
                    "An error occurred while trying to view applications.");
        }
    }
    
    private void editJob(ObjectId jobId) {
        try {
            // Navigate to edit job screen
            // TODO: Implement edit job functionality
            showAlert(Alert.AlertType.INFORMATION, "Information", "Not Implemented", 
                    "Edit job functionality is not implemented yet.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Navigation Error", 
                    "An error occurred while trying to edit the job.");
        }
    }
    
    private void deleteJob(ObjectId jobId) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Job");
        confirmAlert.setContentText("Are you sure you want to delete this job listing? This action cannot be undone.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean success = jobService.deleteJob(jobId);
                    
                    if (success) {
                        // Reload jobs to reflect deletion
                        loadJobs();
                        
                        // Show success message
                        showAlert(Alert.AlertType.INFORMATION, "Success", "Job Deleted", 
                                "The job listing has been deleted successfully.");
                    } else {
                        // Show error
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to Delete Job", 
                                "An error occurred while trying to delete the job. Please try again.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to Delete Job", 
                            "An error occurred: " + e.getMessage());
                }
            }
        });
    }
    
    @FXML
    private void onPostNewJobClick() {
        ((EmployerDashboardController) parentController).loadPostJob();
    }
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
