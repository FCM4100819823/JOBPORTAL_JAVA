package com.jobportal.controllers.jobseeker;

import com.jobportal.App;
import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.services.ApplicationService;
import com.jobportal.services.JobService;
import com.jobportal.services.UserService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SavedJobsController implements Initializable {
    
    @FXML
    private ComboBox<String> categoryFilterComboBox;
    
    @FXML
    private ComboBox<String> sortComboBox;
    
    @FXML
    private VBox savedJobsContainer;
    
    @FXML
    private StackPane loadingPane;
    
    @FXML
    private VBox emptyStatePane;
    
    private DashboardController parentController;
    private UserService userService;
    private JobService jobService;
    private ApplicationService applicationService;
    
    private List<Document> allSavedJobs;
    private Map<ObjectId, Date> savedDates;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        jobService = new JobService();
        applicationService = new ApplicationService();
        
        // Initialize sort options
        initializeSortOptions();
    }
    
    private void initializeSortOptions() {
        sortComboBox.getItems().addAll(
                "Recently Saved",
                "Oldest Saved",
                "Company Name (A-Z)",
                "Job Title (A-Z)"
        );
        
        sortComboBox.getSelectionModel().select(0);
        
        sortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                filterAndDisplayJobs();
            }
        });
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        loadSavedJobs();
    }
    
    private void loadSavedJobs() {
        if (parentController == null || parentController.currentUser == null) {
            return;
        }
        
        // Show loading
        loadingPane.setVisible(true);
        loadingPane.setManaged(true);
        emptyStatePane.setVisible(false);
        emptyStatePane.setManaged(false);
        
        // Clear existing jobs
        savedJobsContainer.getChildren().removeIf(node -> node.getStyleClass().contains("job-card"));
        
        // Get saved jobs in background
        Task<List<Document>> task = new Task<>() {
            @Override
            protected List<Document> call() {
                ObjectId userId = parentController.currentUser.getId();
                
                // Get all saved jobs with metadata
                allSavedJobs = userService.getSavedJobsWithMetadata(userId);
                
                // Create a map of jobId -> savedDate for quick lookup
                savedDates = allSavedJobs.stream()
                        .collect(Collectors.toMap(
                                doc -> doc.getObjectId("jobId"),
                                doc -> doc.getDate("savedAt")
                        ));
                
                // Get job categories for filter options
                Map<String, Integer> categories = userService.categorizeSavedJobs(userId);
                
                // Update filter options on UI thread
                Platform.runLater(() -> updateFilterOptions(categories));
                
                return allSavedJobs;
            }
        };
        
        task.setOnSucceeded(event -> {
            List<Document> savedJobs = task.getValue();
            filterAndDisplayJobs();
        });
        
        task.setOnFailed(event -> {
            // Hide loading
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            
            // Show error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to Load Saved Jobs");
            alert.setContentText("An error occurred while loading your saved jobs. Please try again.");
            alert.showAndWait();
        });
        
        new Thread(task).start();
    }
    
    private void updateFilterOptions(Map<String, Integer> categories) {
        categoryFilterComboBox.getItems().clear();
        
        // Add categories with counts
        for (Map.Entry<String, Integer> entry : categories.entrySet()) {
            categoryFilterComboBox.getItems().add(entry.getKey() + " (" + entry.getValue() + ")");
        }
        
        // Set default filter to All
        categoryFilterComboBox.getSelectionModel().select(0);
        
        // Add listener for filter changes
        categoryFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                filterAndDisplayJobs();
            }
        });
    }
    
    private void filterAndDisplayJobs() {
        if (allSavedJobs == null || allSavedJobs.isEmpty()) {
            // Hide loading
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            
            // Show empty state
            emptyStatePane.setVisible(true);
            emptyStatePane.setManaged(true);
            return;
        }
        
        // Get selected category
        String selectedCategory = categoryFilterComboBox.getValue();
        if (selectedCategory == null) {
            selectedCategory = "All";
        } else {
            // Extract category name without count
            selectedCategory = selectedCategory.split(" \\(")[0];
        }
        
        // Filter jobs based on category
        List<Job> filteredJobs = new ArrayList<>();
        for (Document doc : allSavedJobs) {
            ObjectId jobId = doc.getObjectId("jobId");
            Job job = jobService.getJobById(jobId);
            
            if (job != null) {
                // Apply category filter
                if (selectedCategory.equals("All") ||
                        (selectedCategory.equals("Recent") && isRecentlySaved(doc.getDate("savedAt"))) ||
                        (job.getEmploymentType() != null && job.getEmploymentType().equals(selectedCategory))) {
                    filteredJobs.add(job);
                }
            }
        }
        
        // Sort filtered jobs
        sortJobs(filteredJobs);
        
        // Display filtered and sorted jobs
        displaySavedJobs(filteredJobs);
    }
    
    private boolean isRecentlySaved(Date savedDate) {
        if (savedDate == null) return false;
        
        long daysDiff = (new Date().getTime() - savedDate.getTime()) / (1000 * 60 * 60 * 24);
        return daysDiff <= 7; // Within last 7 days
    }
    
    private void sortJobs(List<Job> jobs) {
        String sortOption = sortComboBox.getValue();
        if (sortOption == null) return;
        
        switch (sortOption) {
            case "Recently Saved" -> jobs.sort((j1, j2) -> {
                Date date1 = savedDates.get(j1.getId());
                Date date2 = savedDates.get(j2.getId());
                return date2.compareTo(date1); // Newest first
            });
            case "Oldest Saved" -> jobs.sort((j1, j2) -> {
                Date date1 = savedDates.get(j1.getId());
                Date date2 = savedDates.get(j2.getId());
                return date1.compareTo(date2); // Oldest first
            });
            case "Company Name (A-Z)" -> jobs.sort((j1, j2) -> {
                String company1 = j1.getCompany() != null ? j1.getCompany() : "";
                String company2 = j2.getCompany() != null ? j2.getCompany() : "";
                return company1.compareToIgnoreCase(company2);
            });
            case "Job Title (A-Z)" -> jobs.sort((j1, j2) -> {
                String title1 = j1.getTitle() != null ? j1.getTitle() : "";
                String title2 = j2.getTitle() != null ? j2.getTitle() : "";
                return title1.compareToIgnoreCase(title2);
            });
        }
    }
    
    private void displaySavedJobs(List<Job> jobs) {
        Platform.runLater(() -> {
            // Hide loading
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            
            // Clear existing job cards
            savedJobsContainer.getChildren().removeIf(node -> node.getStyleClass().contains("job-card"));
            
            if (jobs.isEmpty()) {
                // Show empty state
                emptyStatePane.setVisible(true);
                emptyStatePane.setManaged(true);
                return;
            }
            
            // Hide empty state
            emptyStatePane.setVisible(false);
            emptyStatePane.setManaged(false);
            
            // Create job cards for each saved job
            for (Job job : jobs) {
                try {
                    Node jobCard = createSavedJobCard(job);
                    savedJobsContainer.getChildren().add(jobCard);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    private Node createSavedJobCard(Job job) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/components/savedJobCard.fxml"));
        Node card = loader.load();
        
        // Set job details
        Text jobTitleText = (Text) card.lookup("#jobTitleText");
        Text companyText = (Text) card.lookup("#companyText");
        Text locationText = (Text) card.lookup("#locationText");
        Text jobTypeText = (Text) card.lookup("#jobTypeText");
        
        jobTitleText.setText(job.getTitle());
        companyText.setText(job.getCompany());
        locationText.setText(job.getLocation());
        jobTypeText.setText(job.getEmploymentType());
        
        // Set salary if available
        HBox salaryContainer = (HBox) card.lookup("#salaryContainer");
        Text salaryText = (Text) card.lookup("#salaryText");
        if (job.getSalaryMin() != null) {
            String salaryRange = formatSalaryRange(job.getSalaryMin(), job.getSalaryMax(), job.getCurrency());
            salaryText.setText(salaryRange);
        } else {
            salaryContainer.setVisible(false);
        }
        
        // Set saved date
        Text savedDateText = (Text) card.lookup("#savedDateText");
        Date savedDate = savedDates.get(job.getId());
        if (savedDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy");
            savedDateText.setText("Saved on " + dateFormat.format(savedDate));
        } else {
            savedDateText.setText("Recently saved");
        }
        
        // Configure buttons
        Button removeButton = (Button) card.lookup("#removeButton");
        Button viewDetailsButton = (Button) card.lookup("#viewDetailsButton");
        Button applyButton = (Button) card.lookup("#applyButton");
        
        // Set button actions
        removeButton.setOnAction(e -> unsaveJob(job.getId()));
        viewDetailsButton.setOnAction(e -> viewJobDetails(job.getId().toString()));
        applyButton.setOnAction(e -> applyForJob(job.getId().toString()));
        
        // Disable apply button if already applied
        if (applicationService.hasApplied(job.getId(), parentController.currentUser.getId())) {
            applyButton.setText("Applied");
            applyButton.setDisable(true);
        }
        
        return card;
    }
    
    private void unsaveJob(ObjectId jobId) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Remove Saved Job");
        confirmAlert.setHeaderText("Remove from Saved Jobs");
        confirmAlert.setContentText("Are you sure you want to remove this job from your saved jobs?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = userService.unsaveJob(parentController.currentUser.getId(), jobId);
                if (success) {
                    // Reload saved jobs
                    loadSavedJobs();
                }
            }
        });
    }
    
    private void viewJobDetails(String jobId) {
        try {
            // Navigate to job details page
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/jobDetail.fxml"));
            Node jobDetailView = loader.load();
            
            // Initialize controller
            JobDetailController controller = loader.getController();
            controller.setParentController(parentController);
            controller.loadJob(jobId, "saved");
            
            // Display job details view
            parentController.contentArea.getChildren().clear();
            parentController.contentArea.getChildren().add(jobDetailView);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Failed to load job details.");
        }
    }
    
    private void applyForJob(String jobId) {
        // Navigate directly to the application form for this job
        viewJobDetails(jobId);
    }
    
    @FXML
    private void onBrowseJobsClick() {
        // Navigate to browse jobs screen
        if (parentController instanceof JobSeekerDashboardController) {
            ((JobSeekerDashboardController) parentController).loadBrowseJobs();
        }
    }
    
    private String formatSalaryRange(Double min, Double max, String currency) {
        String currencySymbol = currency != null ? currency : "$";
        
        if (min != null && max != null) {
            return currencySymbol + formatSalary(min.intValue()) + " - " + currencySymbol + formatSalary(max.intValue());
        } else if (min != null) {
            return currencySymbol + formatSalary(min.intValue()) + "+";
        } else {
            return "Salary not specified";
        }
    }
    
    private String formatSalary(int salary) {
        return String.format("%,d", salary);
    }
    
    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
