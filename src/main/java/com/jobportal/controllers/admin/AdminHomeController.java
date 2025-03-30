package com.jobportal.controllers.admin;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.models.User;
import com.jobportal.services.ApplicationService;
import com.jobportal.services.JobService;
import com.jobportal.services.UserService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AdminHomeController implements Initializable {
    
    @FXML
    private Text welcomeMessage;
    
    @FXML
    private Text totalUsersCount;
    
    @FXML
    private Text totalJobsCount;
    
    @FXML
    private Text totalApplicationsCount;
    
    @FXML
    private Text newUsersCount;
    
    // Recent Users Table
    @FXML
    private TableView<User> recentUsersTable;
    
    @FXML
    private TableColumn<User, String> userIdColumn;
    
    @FXML
    private TableColumn<User, String> usernameColumn;
    
    @FXML
    private TableColumn<User, String> fullNameColumn;
    
    @FXML
    private TableColumn<User, String> emailColumn;
    
    @FXML
    private TableColumn<User, String> roleColumn;
    
    @FXML
    private TableColumn<User, String> registrationDateColumn;
    
    // Recent Jobs Table
    @FXML
    private TableView<Job> recentJobsTable;
    
    @FXML
    private TableColumn<Job, String> jobIdColumn;
    
    @FXML
    private TableColumn<Job, String> jobTitleColumn;
    
    @FXML
    private TableColumn<Job, String> companyColumn;
    
    @FXML
    private TableColumn<Job, String> locationColumn;
    
    @FXML
    private TableColumn<Job, String> postDateColumn;
    
    @FXML
    private TableColumn<Job, String> statusColumn;
    
    private DashboardController parentController;
    private UserService userService;
    private JobService jobService;
    private ApplicationService applicationService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        jobService = new JobService();
        applicationService = new ApplicationService();
        
        // Initialize table columns
        initializeUserTableColumns();
        initializeJobTableColumns();
    }
    
    private void initializeUserTableColumns() {
        userIdColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getId() != null ? data.getValue().getId().toString() : ""));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        roleColumn.setCellValueFactory(data -> {
            User.Role role = data.getValue().getRole();
            return new SimpleStringProperty(role != null ? role.toString() : "");
        });
        
        registrationDateColumn.setCellValueFactory(data -> {
            LocalDateTime date = data.getValue().getRegistrationDate();
            if (date == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        });
    }
    
    private void initializeJobTableColumns() {
        jobIdColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getId() != null ? data.getValue().getId().toString() : ""));
        jobTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        companyColumn.setCellValueFactory(new PropertyValueFactory<>("company"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        
        postDateColumn.setCellValueFactory(data -> {
            LocalDateTime date = data.getValue().getPostDate();
            if (date == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        });
        
        statusColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
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
                "! Here's an overview of the platform activities.");
        
        // Load platform stats
        loadPlatformStats();
        
        // Load recent users and jobs
        loadRecentUsers();
        loadRecentJobs();
    }
    
    private void loadPlatformStats() {
        // Create a background task to avoid freezing the UI
        Task<AdminStats> statsTask = new Task<>() {
            @Override
            protected AdminStats call() throws Exception {
                AdminStats stats = new AdminStats();
                
                // Get total users count
                stats.totalUsers = userService.getUserCount();
                
                // Get total jobs count
                stats.totalJobs = jobService.getTotalJobCount();
                
                // Get total applications count
                stats.totalApplications = applicationService.getTotalApplicationCount();
                
                // Get new users count (last 24 hours)
                stats.newUsers24h = userService.getNewUserCount(24);
                
                return stats;
            }
        };
        
        statsTask.setOnSucceeded(event -> {
            AdminStats stats = statsTask.getValue();
            
            // Update UI with stats
            Platform.runLater(() -> {
                totalUsersCount.setText(String.valueOf(stats.totalUsers));
                totalJobsCount.setText(String.valueOf(stats.totalJobs));
                totalApplicationsCount.setText(String.valueOf(stats.totalApplications));
                newUsersCount.setText(String.valueOf(stats.newUsers24h));
            });
        });
        
        statsTask.setOnFailed(event -> {
            Throwable exception = statsTask.getException();
            exception.printStackTrace();
        });
        
        // Start the task
        new Thread(statsTask).start();
    }
    
    private void loadRecentUsers() {
        // Create a background task to avoid freezing the UI
        Task<List<User>> usersTask = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                // Get 10 most recent users
                return userService.getRecentUsers(10);
            }
        };
        
        usersTask.setOnSucceeded(event -> {
            List<User> users = usersTask.getValue();
            
            // Update UI with users
            Platform.runLater(() -> {
                recentUsersTable.setItems(FXCollections.observableArrayList(users));
            });
        });
        
        usersTask.setOnFailed(event -> {
            Throwable exception = usersTask.getException();
            exception.printStackTrace();
        });
        
        // Start the task
        new Thread(usersTask).start();
    }
    
    private void loadRecentJobs() {
        // Create a background task to avoid freezing the UI
        Task<List<Job>> jobsTask = new Task<>() {
            @Override
            protected List<Job> call() throws Exception {
                // Get 10 most recent jobs
                return jobService.getRecentJobs(10);
            }
        };
        
        jobsTask.setOnSucceeded(event -> {
            List<Job> jobs = jobsTask.getValue();
            
            // Update UI with jobs
            Platform.runLater(() -> {
                recentJobsTable.setItems(FXCollections.observableArrayList(jobs));
            });
        });
        
        jobsTask.setOnFailed(event -> {
            Throwable exception = jobsTask.getException();
            exception.printStackTrace();
        });
        
        // Start the task
        new Thread(jobsTask).start();
    }
    
    @FXML
    private void onManageUsersClick() {
        ((AdminDashboardController) parentController).loadUserManagement();
    }
    
    @FXML
    private void onReviewJobsClick() {
        ((AdminDashboardController) parentController).loadJobManagement();
    }
    
    @FXML
    private void onViewAnalyticsClick() {
        ((AdminDashboardController) parentController).loadAnalytics();
    }
    
    @FXML
    private void onViewAllUsersClick() {
        ((AdminDashboardController) parentController).loadUserManagement();
    }
    
    @FXML
    private void onViewAllJobsClick() {
        ((AdminDashboardController) parentController).loadJobManagement();
    }
    
    // Helper class for storing admin stats
    private static class AdminStats {
        int totalUsers;
        int totalJobs;
        int totalApplications;
        int newUsers24h;
    }
}
