package com.jobportal.controllers.employer;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.models.JobApplication;
import com.jobportal.services.ApplicationService;
import com.jobportal.services.JobService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class EmployerHomeController implements Initializable {
    
    @FXML
    private Text welcomeMessage;
    
    @FXML
    private Text activeJobsCount;
    
    @FXML
    private Text totalApplicationsCount;
    
    @FXML
    private Text newApplicationsCount;
    
    @FXML
    private VBox recentApplicationsContainer;
    
    @FXML
    private Label noApplicationsLabel;
    
    @FXML
    private VBox activeJobsContainer;
    
    @FXML
    private Label noJobsLabel;
    
    private DashboardController parentController;
    private JobService jobService;
    private ApplicationService applicationService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobService = new JobService();
        applicationService = new ApplicationService();
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        
        // Now that we have the parent controller, we can access the current user
        loadDashboardData();
    }
    
    private void loadDashboardData() {
        if (parentController == null || parentController.currentUser == null) {
            return;
        }
        
        // Set welcome message
        welcomeMessage.setText("Hello, " + parentController.currentUser.getFullName() + 
                "! Here's an overview of your recruitment activities.");
        
        // Load employer stats
        loadEmployerStats();
        
        // Load recent applications
        loadRecentApplications();
        
        // Load active jobs
        loadActiveJobs();
    }
    
    private void loadEmployerStats() {
        if (parentController.currentUser == null) return;
        
        // Get employer ID
        org.bson.types.ObjectId employerId = parentController.currentUser.getId();
        
        // Get active jobs count
        List<Job> jobs = jobService.getJobsByEmployer(employerId);
        long activeJobsCountValue = jobs.stream().filter(Job::isActive).count();
        activeJobsCount.setText(String.valueOf(activeJobsCountValue));
        
        // Get total applications count
        int totalApplications = 0;
        int newApplications = 0;
        
        for (Job job : jobs) {
            List<JobApplication> applications = applicationService.getApplicationsForJob(job.getId());
            totalApplications += applications.size();
            
            // Count new (submitted) applications
            newApplications += applications.stream()
                    .filter(app -> app.getStatus() == JobApplication.Status.SUBMITTED)
                    .count();
        }
        
        totalApplicationsCount.setText(String.valueOf(totalApplications));
        newApplicationsCount.setText(String.valueOf(newApplications));
    }
    
    private void loadRecentApplications() {
        // Load most recent applications across all jobs
        // This is a placeholder - in a real implementation, you would
        // query the database for the most recent applications
        
        noApplicationsLabel.setVisible(true);
    }
    
    private void loadActiveJobs() {
        if (parentController.currentUser == null) return;
        
        // Get employer ID
        org.bson.types.ObjectId employerId = parentController.currentUser.getId();
        
        // Get active jobs
        List<Job> jobs = jobService.getJobsByEmployer(employerId);
        List<Job> activeJobs = jobs.stream().filter(Job::isActive).toList();
        
        if (activeJobs.isEmpty()) {
            noJobsLabel.setVisible(true);
            return;
        }
        
        noJobsLabel.setVisible(false);
        activeJobsContainer.getChildren().clear();
        
        // Add active jobs to container (limited to 3)
        int limit = Math.min(activeJobs.size(), 3);
        for (int i = 0; i < limit; i++) {
            // In a real implementation, you would create a job card component
            Label jobLabel = new Label(activeJobs.get(i).getTitle() + " at " + activeJobs.get(i).getCompany());
            activeJobsContainer.getChildren().add(jobLabel);
        }
    }
    
    @FXML
    private void onPostJobClick() {
        ((EmployerDashboardController) parentController).loadPostJob();
    }
    
    @FXML
    private void onViewApplicationsClick() {
        ((EmployerDashboardController) parentController).loadApplications();
    }
    
    @FXML
    private void onManageJobsClick() {
        ((EmployerDashboardController) parentController).loadManageJobs();
    }
    
    @FXML
    private void onViewAllApplicationsClick() {
        ((EmployerDashboardController) parentController).loadApplications();
    }
    
    @FXML
    private void onViewAllJobsClick() {
        ((EmployerDashboardController) parentController).loadManageJobs();
    }
}
