package com.jobportal.controllers.jobseeker;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.services.JobService;
import com.jobportal.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.bson.types.ObjectId;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardHomeController implements Initializable {
    
    @FXML
    private Text welcomeMessage;
    
    @FXML
    private Text appliedJobsCount;
    
    @FXML
    private Text savedJobsCount;
    
    @FXML
    private Text profileCompletion;
    
    @FXML
    private VBox recentActivityContainer;
    
    @FXML
    private Label noActivityLabel;
    
    @FXML
    private VBox recommendedJobsContainer;
    
    @FXML
    private Label noRecommendedLabel;
    
    private DashboardController parentController;
    private JobService jobService;
    private UserService userService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobService = new JobService();
        userService = new UserService();
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        
        // Now that we have the parent controller, we can access the current user
        // and load personalized data
        loadDashboardData();
    }
    
    private void loadDashboardData() {
        if (parentController == null || parentController.currentUser == null) {
            return;
        }
        
        // Set welcome message
        welcomeMessage.setText("Hello, " + parentController.currentUser.getFullName() + 
                "! Here's what's happening with your job search.");
        
        // Load stats
        loadUserStats();
        
        // Load recommended jobs
        loadRecommendedJobs();
        
        // Load recent activity
        loadRecentActivity();
    }
    
    private void loadUserStats() {
        ObjectId userId = parentController.currentUser.getId();
        
        // Get application count
        int applications = userService.getApplicationCount(userId);
        appliedJobsCount.setText(String.valueOf(applications));
        
        // Get saved jobs count
        int savedJobs = userService.getSavedJobsCount(userId);
        savedJobsCount.setText(String.valueOf(savedJobs));
        
        // Calculate profile completion
        int completion = userService.calculateProfileCompletion(parentController.currentUser);
        profileCompletion.setText(completion + "%");
    }
    
    private void loadRecommendedJobs() {
        // Get recommended jobs based on user profile, skills, etc.
        List<Job> jobs = jobService.getRecommendedJobs(parentController.currentUser);
        
        if (jobs.isEmpty()) {
            noRecommendedLabel.setVisible(true);
            return;
        }
        
        noRecommendedLabel.setVisible(false);
        recommendedJobsContainer.getChildren().clear();
        
        // Add job cards (limited to 3)
        int limit = Math.min(jobs.size(), 3);
        for (int i = 0; i < limit; i++) {
            Job job = jobs.get(i);
            HBox jobCard = createJobCard(job);
            recommendedJobsContainer.getChildren().add(jobCard);
        }
    }
    
    private void loadRecentActivity() {
        // This would load recent applications, saved jobs, etc.
        // For now, just show the no activity label
        noActivityLabel.setVisible(true);
    }
    
    private HBox createJobCard(Job job) {
        // This would create a job card UI component
        // For simplicity, we'll just return a placeholder
        HBox card = new HBox();
        card.getStyleClass().add("job-card");
        
        // In a real implementation, you would add job details here
        
        return card;
    }
    
    @FXML
    private void onBrowseAllJobsClick() {
        // Navigate to browse jobs screen
        if (parentController != null) {
            ((JobSeekerDashboardController) parentController).loadBrowseJobs();
        }
    }
}
