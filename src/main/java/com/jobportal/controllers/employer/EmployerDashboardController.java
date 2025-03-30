package com.jobportal.controllers.employer;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.User;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class EmployerDashboardController extends DashboardController implements Initializable {
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // This is called by JavaFX when the view is loaded
    }
    
    @Override
    public void initialize(User user) {
        super.initialize(user);
        
        // Set up navigation for Employer
        addNavButton("dashboard", "Dashboard", "fas-tachometer-alt", 
                () -> loadDashboardHome());
        
        addNavButton("postJob", "Post a Job", "fas-plus-circle", 
                () -> loadPostJob());
        
        addNavButton("manageJobs", "Manage Jobs", "fas-briefcase", 
                () -> loadManageJobs());
        
        addNavButton("applications", "Applications", "fas-users", 
                () -> loadApplications());
        
        addNavButton("company", "Company Profile", "fas-building", 
                () -> loadCompanyProfile());
        
        addNavButton("settings", "Settings", "fas-cog", 
                () -> loadSettings());
        
        // Set the dashboard as active by default
        setActiveNav("dashboard");
        loadDashboardHome();
    }
    
    public void loadDashboardHome() {
        setPageTitle("Employer Dashboard");
        loadContent("/fxml/employer/dashboardHome.fxml");
    }
    
    public void loadPostJob() {
        setPageTitle("Post a Job");
        loadContent("/fxml/employer/postJob.fxml");
    }
    
    public void loadManageJobs() {
        setPageTitle("Manage Jobs");
        loadContent("/fxml/employer/manageJobs.fxml");
    }
    
    public void loadApplications() {
        setPageTitle("View Applications");
        loadContent("/fxml/employer/applications.fxml");
    }
    
    public void loadCompanyProfile() {
        setPageTitle("Company Profile");
        loadContent("/fxml/employer/companyProfile.fxml");
    }
    
    public void loadSettings() {
        setPageTitle("Settings");
        loadContent("/fxml/employer/settings.fxml");
    }
}
