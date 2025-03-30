package com.jobportal.controllers.jobseeker;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class JobSeekerDashboardController extends DashboardController implements Initializable {
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // This is called by JavaFX when the view is loaded
        // We'll set up the navigation buttons here, but we need to wait for the user data
    }
    
    @Override
    public void initialize(User user) {
        super.initialize(user);
        
        // Set up navigation for Job Seeker
        addNavButton("dashboard", "Dashboard", "fas-tachometer-alt", 
                () -> loadDashboardHome());
        
        addNavButton("browse", "Browse Jobs", "fas-search", 
                () -> loadBrowseJobs());
        
        addNavButton("applications", "My Applications", "fas-clipboard-list", 
                () -> loadMyApplications());
        
        addNavButton("saved", "Saved Jobs", "fas-bookmark", 
                () -> loadSavedJobs());
        
        addNavButton("profile", "My Profile", "fas-user", 
                () -> loadProfile());
        
        addNavButton("settings", "Settings", "fas-cog", 
                () -> loadSettings());
        
        // Set the dashboard as active by default
        setActiveNav("dashboard");
        loadDashboardHome();
    }
    
    public void loadDashboardHome() {
        setPageTitle("Dashboard");
        loadContent("/fxml/jobseeker/dashboardHome.fxml");
    }
    
    public void loadBrowseJobs() {
        setPageTitle("Browse Jobs");
        loadContent("/fxml/jobseeker/browseJobs.fxml");
    }
    
    private void loadMyApplications() {
        setPageTitle("My Applications");
        loadContent("/fxml/jobseeker/myApplications.fxml");
    }
    
    private void loadSavedJobs() {
        setPageTitle("Saved Jobs");
        loadContent("/fxml/jobseeker/savedJobs.fxml");
    }
    
    private void loadProfile() {
        setPageTitle("My Profile");
        loadContent("/fxml/jobseeker/profile.fxml");
    }
    
    private void loadSettings() {
        setPageTitle("Settings");
        loadContent("/fxml/jobseeker/settings.fxml");
    }
    
    @Override
    protected void handleSearch(String searchTerm) {
        // If we're not already on the browse jobs page, go there
        if (!activeNavId.equals("browse")) {
            setActiveNav("browse");
            loadBrowseJobs();
        }
        
        // Now we need to pass the search term to the browse jobs controller
        // This would be implemented in the loadContent method to pass data to the loaded controller
    }
}
