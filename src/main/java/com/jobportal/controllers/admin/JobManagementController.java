package com.jobportal.controllers.admin;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.models.User;
import com.jobportal.services.JobService;
import com.jobportal.services.UserService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.bson.types.ObjectId;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class JobManagementController implements Initializable {
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> statusFilterComboBox;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private TableView<Job> jobsTable;
    
    @FXML
    private TableColumn<Job, String> jobIdColumn;
    
    @FXML
    private TableColumn<Job, String> titleColumn;
    
    @FXML
    private TableColumn<Job, String> companyColumn;
    
    @FXML
    private TableColumn<Job, String> locationColumn;
    
    @FXML
    private TableColumn<Job, String> typeColumn;
    
    @FXML
    private TableColumn<Job, String> employerColumn;
    
    @FXML
    private TableColumn<Job, String> postedDateColumn;
    
    @FXML
    private TableColumn<Job, String> statusColumn;
    
    @FXML
    private TableColumn<Job, Void> actionsColumn;
    
    @FXML
    private Pagination jobsPagination;
    
    private DashboardController parentController;
    private JobService jobService;
    private UserService userService;
    
    private String searchQuery = "";
    private Boolean activeFilter = null;
    private int itemsPerPage = 20;
    private int totalJobs = 0;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobService = new JobService();
        userService = new UserService();
        
        // Initialize status filter
        statusFilterComboBox.getItems().addAll(
                "All Jobs",
                "Active Jobs",
                "Inactive Jobs"
        );
        statusFilterComboBox.getSelectionModel().select(0);
        
        // Set up table columns
        initializeTableColumns();
        
        // Set up pagination
        jobsPagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
            loadJobs(newVal.intValue());
        });
        
        // Set up status filter listener
        statusFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal.equals("All Jobs")) {
                    activeFilter = null;
                } else if (newVal.equals("Active Jobs")) {
                    activeFilter = true;
                } else if (newVal.equals("Inactive Jobs")) {
                    activeFilter = false;
                }
                
                // Reset pagination to first page and reload jobs
                jobsPagination.setCurrentPageIndex(0);
                loadJobs(0);
            }
        });
    }
    
    private void initializeTableColumns() {
        jobIdColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getId() != null ? data.getValue().getId().toString() : ""));
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        companyColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCompany()));
        locationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLocation()));
        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmploymentType()));
        
        employerColumn.setCellValueFactory(data -> {
            ObjectId employerId = data.getValue().getEmployerId();
            if (employerId != null) {
                User employer = userService.getUserById(employerId);
                return new SimpleStringProperty(employer != null ? employer.getFullName() : "Unknown");
            }
            return new SimpleStringProperty("Unknown");
        });
        
        postedDateColumn.setCellValueFactory(data -> {
            LocalDateTime date = data.getValue().getPostDate();
            if (date == null) return new SimpleStringProperty("");
            return new SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        });
        
        statusColumn.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
        
        // Set up actions column with toggle and delete buttons
        actionsColumn.setCellFactory(createActionsColumnCellFactory());
    }
    
    private Callback<TableColumn<Job, Void>, TableCell<Job, Void>> createActionsColumnCellFactory() {
        return param -> new TableCell<>() {
            private final Button toggleButton = new Button();
            private final Button deleteButton = new Button();
            
            {
                toggleButton.getStyleClass().addAll("button-icon", "button-toggle");
                toggleButton.setTooltip(new Tooltip("Toggle Status"));
                
                deleteButton.getStyleClass().add("button-icon");
                deleteButton.setGraphic(new FontIcon("fas-trash-alt"));
                deleteButton.setTooltip(new Tooltip("Delete Job"));
                
                // Set button actions
                toggleButton.setOnAction(event -> {
                    Job job = getTableView().getItems().get(getIndex());
                    toggleJobStatus(job);
                });
                
                deleteButton.setOnAction(event -> {
                    Job job = getTableView().getItems().get(getIndex());
                    deleteJob(job);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    Job job = getTableView().getItems().get(getIndex());
                    
                    // Update toggle button icon based on job status
                    if (job.isActive()) {
                        toggleButton.setGraphic(new FontIcon("fas-toggle-on"));
                    } else {
                        toggleButton.setGraphic(new FontIcon("fas-toggle-off"));
                    }
                    
                    HBox buttonsBox = new HBox(10, toggleButton, deleteButton);
                    setGraphic(buttonsBox);
                }
            }
        };
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        
        // Load jobs
        loadJobs(0);
    }
    
    @FXML
    private void onSearchButtonClick() {
        searchQuery = searchField.getText().trim();
        jobsPagination.setCurrentPageIndex(0);
        loadJobs(0);
    }
    
    private void loadJobs(int page) {
        // Create a background task
        Task<JobData> jobsTask = new Task<>() {
            @Override
            protected JobData call() throws Exception {
                JobData data = new JobData();
                
                // Get total count for pagination
                data.totalCount = jobService.getJobsCount(searchQuery, activeFilter);
                
                // Get paginated jobs
                data.jobs = jobService.getPaginatedJobs(
                        searchQuery, 
                        activeFilter, 
                        page, 
                        itemsPerPage
                );
                
                return data;
            }
        };
        
        jobsTask.setOnSucceeded(event -> {
            JobData data = jobsTask.getValue();
            totalJobs = data.totalCount;
            
            // Update pagination
            int pageCount = (int) Math.ceil((double) totalJobs / itemsPerPage);
            jobsPagination.setPageCount(Math.max(1, pageCount));
            
            // Update table
            Platform.runLater(() -> {
                jobsTable.setItems(FXCollections.observableArrayList(data.jobs));
            });
        });
        
        jobsTask.setOnFailed(event -> {
            Throwable exception = jobsTask.getException();
            exception.printStackTrace();
            
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load jobs", 
                    "An error occurred while loading jobs: " + exception.getMessage());
        });
        
        // Start the task
        new Thread(jobsTask).start();
    }
    
    private void toggleJobStatus(Job job) {
        try {
            // Toggle active status
            job.setActive(!job.isActive());
            boolean success = jobService.updateJob(job);
            
            if (success) {
                // Reload to reflect changes
                loadJobs(jobsPagination.getCurrentPageIndex());
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Job Status Updated", 
                        "The job status has been updated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to Update Job Status", 
                        "An error occurred while trying to update the job status.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to Update Job Status", 
                    "An error occurred: " + e.getMessage());
        }
    }
    
    private void deleteJob(Job job) {
        // Confirm deletion
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Job");
        confirmDialog.setContentText("Are you sure you want to delete the job '" + job.getTitle() + "'? This action cannot be undone.");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = jobService.deleteJob(job.getId());
                
                if (success) {
                    // Reload jobs
                    loadJobs(jobsPagination.getCurrentPageIndex());
                    
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Job Deleted", 
                            "The job has been deleted successfully.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to Delete Job", 
                            "An error occurred while trying to delete the job.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to Delete Job", 
                        "An error occurred: " + e.getMessage());
            }
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    // Helper class for job data
    private static class JobData {
        List<Job> jobs;
        int totalCount;
    }
}
