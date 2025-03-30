package com.jobportal.controllers.admin;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.User;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController extends DashboardController implements Initializable {
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // This is called by JavaFX when the view is loaded
    }
    
    @Override
    public void initialize(User user) {
        super.initialize(user);
        
        // Set up navigation for Admin
        addNavButton("dashboard", "Dashboard", "fas-tachometer-alt", 
                () -> loadDashboardHome());
        
        addNavButton("users", "User Management", "fas-users", 
                () -> loadUserManagement());
        
        addNavButton("jobs", "Job Listings", "fas-briefcase", 
                () -> loadJobManagement());
        
        addNavButton("applications", "Applications", "fas-file-alt", 
                () -> loadApplicationManagement());
        
        addNavButton("analytics", "Analytics", "fas-chart-bar", 
                () -> loadAnalytics());
        
        addNavButton("settings", "System Settings", "fas-cog", 
                () -> loadSettings());
        
        // Set the dashboard as active by default
        setActiveNav("dashboard");
        loadDashboardHome();
    }
    
    public void loadDashboardHome() {
        setPageTitle("Admin Dashboard");
        loadContent("/fxml/admin/dashboardHome.fxml");
    }
    
    public void loadUserManagement() {
        setPageTitle("User Management");
        loadContent("/fxml/admin/userManagement.fxml");
    }
    
    public void loadJobManagement() {
        setPageTitle("Job Listings");
        loadContent("/fxml/admin/jobManagement.fxml");
    }
    
    public void loadApplicationManagement() {
        setPageTitle("Application Management");
        loadContent("/fxml/admin/applicationManagement.fxml");
    }
    
    public void loadAnalytics() {
        setPageTitle("Platform Analytics");
        loadContent("/fxml/admin/analytics.fxml");
    }
    
    public void loadSettings() {
        setPageTitle("System Settings");
        loadContent("/fxml/admin/settings.fxml");
    }
}
