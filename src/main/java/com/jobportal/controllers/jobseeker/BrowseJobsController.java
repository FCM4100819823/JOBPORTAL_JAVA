package com.jobportal.controllers.jobseeker;

import com.jobportal.App;
import com.jobportal.controllers.common.DashboardController;
import com.jobportal.controllers.jobseeker.AdvancedSearchFilterController.JobSearchCriteria;
import com.jobportal.models.Job;
import com.jobportal.services.ApplicationService;
import com.jobportal.services.JobService;
import com.jobportal.services.UserService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.bson.types.ObjectId;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class BrowseJobsController implements Initializable {
    
    @FXML
    private VBox jobListContainer;
    
    @FXML
    private StackPane loadingPane;
    
    @FXML
    private VBox noResultsPane;
    
    @FXML
    private Text resultsCountText;
    
    @FXML
    private ComboBox<String> sortComboBox;
    
    @FXML
    private Pagination jobPagination;
    
    @FXML
    private HBox activeFiltersContainer;
    
    @FXML
    private FlowPane filtersFlowPane;
    
    @FXML
    private Button clearAllFiltersButton;
    
    @FXML
    private Node searchFilters;
    
    private AdvancedSearchFilterController searchFiltersController;
    
    private DashboardController parentController;
    private JobService jobService;
    private UserService userService;
    private ApplicationService applicationService;
    
    private List<Job> currentJobs;
    private int totalJobs;
    private int jobsPerPage = 10;
    private JobSearchCriteria currentSearchCriteria = new JobSearchCriteria();
    
    // Properties for filter clear actions
    private final SimpleBooleanProperty clearAllFilters = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty clearKeyword = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty clearLocation = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty clearRemote = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty clearJobType = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty clearExperience = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty clearSalary = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty clearDate = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty removeSkill = new SimpleBooleanProperty(false);
    private String skillToRemove;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobService = new JobService();
        userService = new UserService();
        applicationService = new ApplicationService();
        
        // Get the search filters controller
        FXMLLoader loader = (FXMLLoader) searchFilters.getProperties().get("fx:loader");
        searchFiltersController = loader.getController();
        
        // Set up listeners
        setupListeners();
        
        // Initialize pagination
        initializePagination();
        
        // Initialize sort combobox
        initializeSortComboBox();
    }
    
    private void setupListeners() {
        // Listen for search criteria changes
        searchFiltersController.searchCriteriaProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSearchCriteria = newVal;
                updateActiveFilters();
                loadJobs(0); // Reset to first page
            }
        });
        
        // Set up clear all filters button
        clearAllFiltersButton.setOnAction(e -> {
            clearAllFilters.set(true);
        });
        
        // Bind property listeners from controller to this
        searchFiltersController.bindClearProperties(
            clearAllFilters, clearKeyword, clearLocation, clearRemote, 
            clearJobType, clearExperience, clearSalary, clearDate, removeSkill
        );
        
        removeSkill.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                searchFiltersController.removeSkill(skillToRemove);
                removeSkill.set(false);
            }
        });
    }
    
    private void initializePagination() {
        jobPagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
            loadJobs(newVal.intValue());
        });
    }
    
    private void initializeSortComboBox() {
        // Add sort options
        sortComboBox.getItems().addAll(
            "Relevance",
            "Date (newest)",
            "Date (oldest)",
            "Salary (highest)",
            "Salary (lowest)"
        );
        
        // Select default
        sortComboBox.getSelectionModel().select(0);
        
        // Listen for changes
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadJobs(jobPagination.getCurrentPageIndex());
            }
        });
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        loadJobs(0);
    }
    
    private void loadJobs(int page) {
        // Show loading indicator
        loadingPane.setVisible(true);
        loadingPane.setManaged(true);
        noResultsPane.setVisible(false);
        noResultsPane.setManaged(false);
        
        // Clear existing job cards
        jobListContainer.getChildren().clear();
        
        // Create a background task to load jobs
        Task<List<Job>> task = new Task<>() {
            @Override
            protected List<Job> call() {
                // Get the sort option
                String sortOption = sortComboBox.getValue();
                
                // Get filtered jobs with pagination
                totalJobs = jobService.getJobsCount(currentSearchCriteria);
                return jobService.searchJobs(
                        currentSearchCriteria,
                        sortOption,
                        page,
                        jobsPerPage
                );
            }
        };
        
        task.setOnSucceeded(event -> {
            currentJobs = task.getValue();
            
            Platform.runLater(() -> {
                // Hide loading indicator
                loadingPane.setVisible(false);
                loadingPane.setManaged(false);
                
                // Check if we have results
                if (currentJobs == null || currentJobs.isEmpty()) {
                    noResultsPane.setVisible(true);
                    noResultsPane.setManaged(true);
                    resultsCountText.setText("No jobs found");
                    return;
                }
                
                // Update results count text
                int startResult = page * jobsPerPage + 1;
                int endResult = Math.min((page + 1) * jobsPerPage, totalJobs);
                resultsCountText.setText(String.format("Showing %d-%d of %d jobs", 
                        startResult, endResult, totalJobs));
                
                // Update pagination
                int pageCount = (int) Math.ceil((double) totalJobs / jobsPerPage);
                jobPagination.setPageCount(Math.max(1, pageCount));
                
                // Display job cards
                displayJobs(currentJobs);
            });
        });
        
        task.setOnFailed(event -> {
            Platform.runLater(() -> {
                // Hide loading indicator
                loadingPane.setVisible(false);
                loadingPane.setManaged(false);
                
                // Show error message
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Failed to load jobs");
                alert.setContentText("An error occurred while loading jobs. Please try again.");
                alert.showAndWait();
            });
        });
        
        // Start the background task
        new Thread(task).start();
    }
    
    private void displayJobs(List<Job> jobs) {
        boolean isLoggedIn = parentController != null && parentController.currentUser != null;
        ObjectId userId = isLoggedIn ? parentController.currentUser.getId() : null;
        
        for (Job job : jobs) {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/components/jobCard.fxml"));
                Node jobCard = loader.load();
                
                // Configure job card
                configureJobCard(jobCard, job, isLoggedIn, userId);
                
                // Add to container
                jobListContainer.getChildren().add(jobCard);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void configureJobCard(Node jobCard, Job job, boolean isLoggedIn, ObjectId userId) {
        // Get card elements
        Label titleLabel = (Label) jobCard.lookup("#jobTitleLabel");
        Label companyLabel = (Label) jobCard.lookup("#companyLabel");
        Label locationLabel = (Label) jobCard.lookup("#locationLabel");
        Label salaryLabel = (Label) jobCard.lookup("#salaryLabel");
        Label jobTypeLabel = (Label) jobCard.lookup("#jobTypeLabel");
        Label postedDateLabel = (Label) jobCard.lookup("#postedDateLabel");
        Label experienceLevelLabel = (Label) jobCard.lookup("#experienceLevelLabel");
        Button saveButton = (Button) jobCard.lookup("#saveButton");
        Button applyButton = (Button) jobCard.lookup("#applyButton");
        
        // Set job details
        titleLabel.setText(job.getTitle());
        companyLabel.setText(job.getCompany());
        locationLabel.setText(job.getLocation());
        
        // Set salary if available
        if (job.getSalaryMin() != null || job.getSalaryMax() != null) {
            salaryLabel.setText(formatSalaryRange(job.getSalaryMin(), job.getSalaryMax(), job.getCurrency()));
        } else {
            salaryLabel.setText("Salary not specified");
        }
        
        // Set job type
        jobTypeLabel.setText(job.getEmploymentType());
        
        // Set posted date
        String postedTime = formatTimeAgo(job.getPostDate());
        postedDateLabel.setText("Posted " + postedTime);
        
        // Set experience level if available
        if (job.getExperienceLevel() != null && !job.getExperienceLevel().isEmpty()) {
            experienceLevelLabel.setText(job.getExperienceLevel());
        } else {
            experienceLevelLabel.setText("Not specified");
        }
        
        // Set up save and apply buttons
        if (isLoggedIn) {
            // Check if job is saved
            boolean isSaved = userService.isJobSaved(userId, job.getId());
            if (isSaved) {
                saveButton.setText("Saved");
                saveButton.getStyleClass().add("button-saved");
            }
            
            // Save button action
            saveButton.setOnAction(e -> toggleSaveJob(saveButton, job.getId()));
            
            // Check if user has already applied
            boolean hasApplied = applicationService.hasApplied(job.getId(), userId);
            if (hasApplied) {
                applyButton.setText("Applied");
                applyButton.setDisable(true);
            } else {
                applyButton.setOnAction(e -> applyForJob(job.getId().toString()));
            }
        } else {
            // If not logged in, prompt login
            saveButton.setOnAction(e -> promptLogin());
            applyButton.setOnAction(e -> promptLogin());
        }
        
        // Set up view details action
        jobCard.setOnMouseClicked(e -> viewJobDetails(job.getId().toString()));
    }
    
    private void toggleSaveJob(Button saveButton, ObjectId jobId) {
        ObjectId userId = parentController.currentUser.getId();
        boolean isSaved = saveButton.getText().equals("Saved");
        
        boolean success;
        if (isSaved) {
            // Unsave the job
            success = userService.unsaveJob(userId, jobId);
            if (success) {
                saveButton.setText("Save");
                saveButton.getStyleClass().remove("button-saved");
            }
        } else {
            // Save the job
            success = userService.saveJob(userId, jobId);
            if (success) {
                saveButton.setText("Saved");
                saveButton.getStyleClass().add("button-saved");
            }
        }
    }
    
    private void applyForJob(String jobId) {
        viewJobDetails(jobId);
    }
    
    private void promptLogin() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Login Required");
        alert.setHeaderText("Please Log In");
        alert.setContentText("You need to be logged in to perform this action.");
        alert.showAndWait();
    }
    
    private void viewJobDetails(String jobId) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/jobDetail.fxml"));
            Node jobDetailView = loader.load();
            
            JobDetailController controller = loader.getController();
            controller.setParentController(parentController);
            controller.loadJob(jobId, "browse");
            
            parentController.contentArea.getChildren().clear();
            parentController.contentArea.getChildren().add(jobDetailView);
        } catch (IOException e) {
            e.printStackTrace();
            // Show error
        }
    }
    
    private void updateActiveFilters() {
        if (currentSearchCriteria.hasActiveFilters()) {
            activeFiltersContainer.setVisible(true);
            activeFiltersContainer.setManaged(true);
            
            // Clear existing filter chips
            filtersFlowPane.getChildren().clear();
            
            // Add filter chips for each active filter
            Map<String, String> activeFilters = currentSearchCriteria.getActiveFilters();
            
            for (Map.Entry<String, String> filter : activeFilters.entrySet()) {
                HBox filterChip = createFilterChip(filter.getKey(), filter.getValue());
                filtersFlowPane.getChildren().add(filterChip);
            }
            
            // Add skill chips
            if (currentSearchCriteria.getSkills() != null) {
                for (String skill : currentSearchCriteria.getSkills()) {
                    HBox skillChip = createFilterChip("Skill", skill);
                    filtersFlowPane.getChildren().add(skillChip);
                }
            }
        } else {
            activeFiltersContainer.setVisible(false);
            activeFiltersContainer.setManaged(false);
        }
    }
    
    private HBox createFilterChip(String key, String value) {
        HBox chip = new HBox(5);
        chip.getStyleClass().add("filter-chip");
        chip.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
        
        Label label = new Label(key + ": " + value);
        Button removeButton = new Button();
        removeButton.getStyleClass().add("chip-remove-button");
        removeButton.setGraphic(new FontIcon("fas-times"));
        removeButton.setOnAction(e -> {
            // Remove this filter and update search
            removeFilter(key, value);
        });
        
        chip.getChildren().addAll(label, removeButton);
        return chip;
    }
    
    private void removeFilter(String key, String value) {
        // Set appropriate property to trigger filter removal in the controller
        switch (key) {
            case "Keyword" -> clearKeyword.set(true);
            case "Location" -> clearLocation.set(true);
            case "Remote" -> clearRemote.set(true);
            case "Job Type" -> clearJobType.set(true);
            case "Experience" -> clearExperience.set(true);
            case "Min Salary" -> clearSalary.set(true);
            case "Date" -> clearDate.set(true);
            case "Skill" -> {
                skillToRemove = value;
                removeSkill.set(true);
            }
        }
    }
    
    private String formatSalaryRange(Double min, Double max, String currency) {
        String currencySymbol = currency != null ? currency : "$";
        
        if (min != null && max != null) {
            return currencySymbol + formatSalary(min.intValue()) + " - " + currencySymbol + formatSalary(max.intValue());
        } else if (min != null) {
            return currencySymbol + formatSalary(min.intValue()) + "+";
        } else if (max != null) {
            return "Up to " + currencySymbol + formatSalary(max.intValue());
        } else {
            return "Salary not specified";
        }
    }
    
    private String formatSalary(int salary) {
        return String.format("%,d", salary);
    }
    
    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "unknown date";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        
        if (minutes < 60) {
            return minutes <= 1 ? "just now" : minutes + " minutes ago";
        }
        
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }
        
        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 7) {
            return days == 1 ? "yesterday" : days + " days ago";
        }
        
        long weeks = days / 7;
        if (weeks < 4) {
            return weeks == 1 ? "1 week ago" : weeks + " weeks ago";
        }
        
        long months = days / 30;
        if (months < 12) {
            return months == 1 ? "1 month ago" : months + " months ago";
        }
        
        long years = days / 365;
        return years == 1 ? "1 year ago" : years + " years ago";
    }
}
