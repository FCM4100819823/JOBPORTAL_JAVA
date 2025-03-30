package com.jobportal.controllers.employer;

import com.jobportal.App;
import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.models.JobApplication;
import com.jobportal.services.ApplicationService;
import com.jobportal.services.JobService;
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
import org.bson.types.ObjectId;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationsController implements Initializable {
    
    @FXML
    private ComboBox<String> jobFilterComboBox;
    
    @FXML
    private ComboBox<String> statusFilterComboBox;
    
    @FXML
    private HBox statsContainer;
    
    @FXML
    private Text totalApplicationsText;
    
    @FXML
    private Text newApplicationsText;
    
    @FXML
    private Text reviewingText;
    
    @FXML
    private Text interviewingText;
    
    @FXML
    private Text offersText;
    
    @FXML
    private Text rejectedText;
    
    @FXML
    private VBox applicationsContainer;
    
    @FXML
    private StackPane loadingPane;
    
    @FXML
    private VBox emptyStatePane;
    
    private DashboardController parentController;
    private JobService jobService;
    private ApplicationService applicationService;
    
    private Map<String, ObjectId> jobIdMap = new HashMap<>();
    private ObjectId selectedJobId = null;
    private JobApplication.Status selectedStatus = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobService = new JobService();
        applicationService = new ApplicationService();
        
        // Initialize status filter options
        initializeStatusFilter();
    }
    
    private void initializeStatusFilter() {
        statusFilterComboBox.getItems().addAll(
                "All Statuses",
                "Submitted",
                "Under Review",
                "Interview Scheduled",
                "Offer Extended",
                "Rejected"
        );
        
        statusFilterComboBox.getSelectionModel().select(0);
        
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // Convert string status to enum
                if (newValue.equals("All Statuses")) {
                    selectedStatus = null;
                } else {
                    selectedStatus = convertToStatusEnum(newValue);
                }
                loadApplications();
            }
        });
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        
        // Initialize job filter with employer's jobs
        initializeJobFilter();
        
        // Load applications
        loadApplicationData();
    }
    
    private void initializeJobFilter() {
        if (parentController == null || parentController.currentUser == null) {
            return;
        }
        
        // Load employer's jobs
        List<Job> jobs = jobService.getJobsByEmployer(parentController.currentUser.getId());
        
        // Clear existing mapping
        jobIdMap.clear();
        
        // Add "All Jobs" option
        jobFilterComboBox.getItems().add("All Jobs");
        
        // Add each job as an option
        for (Job job : jobs) {
            String displayText = job.getTitle() + " at " + job.getCompany();
            jobFilterComboBox.getItems().add(displayText);
            jobIdMap.put(displayText, job.getId());
        }
        
        // Select "All Jobs" by default
        jobFilterComboBox.getSelectionModel().select(0);
        
        // Add listener for job selection changes
        jobFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.equals("All Jobs")) {
                    selectedJobId = null;
                } else {
                    selectedJobId = jobIdMap.get(newValue);
                }
                loadApplications();
            }
        });
    }
    
    private void loadApplicationData() {
        // Load application statistics
        loadApplicationStats();
        
        // Load applications
        loadApplications();
    }
    
    private void loadApplicationStats() {
        if (parentController == null || parentController.currentUser == null) {
            return;
        }
        
        // Start a background task to load stats
        Task<Map<JobApplication.Status, Integer>> statsTask = new Task<>() {
            @Override
            protected Map<JobApplication.Status, Integer> call() {
                ObjectId employerId = parentController.currentUser.getId();
                return applicationService.getApplicationStatsByEmployer(employerId);
            }
        };
        
        statsTask.setOnSucceeded(event -> {
            Map<JobApplication.Status, Integer> stats = statsTask.getValue();
            displayApplicationStats(stats);
        });
        
        new Thread(statsTask).start();
    }
    
    private void displayApplicationStats(Map<JobApplication.Status, Integer> stats) {
        Platform.runLater(() -> {
            // Calculate total
            int total = stats.values().stream().mapToInt(Integer::intValue).sum();
            totalApplicationsText.setText(String.valueOf(total));
            
            // Set stats for specific statuses
            newApplicationsText.setText(String.valueOf(stats.getOrDefault(JobApplication.Status.SUBMITTED, 0)));
            reviewingText.setText(String.valueOf(stats.getOrDefault(JobApplication.Status.UNDER_REVIEW, 0)));
            interviewingText.setText(String.valueOf(stats.getOrDefault(JobApplication.Status.INTERVIEW_SCHEDULED, 0)));
            
            // Combine offer statuses
            int offers = stats.getOrDefault(JobApplication.Status.OFFER_EXTENDED, 0) +
                         stats.getOrDefault(JobApplication.Status.OFFER_ACCEPTED, 0) +
                         stats.getOrDefault(JobApplication.Status.OFFER_DECLINED, 0);
            offersText.setText(String.valueOf(offers));
            
            // Rejections
            rejectedText.setText(String.valueOf(stats.getOrDefault(JobApplication.Status.REJECTED, 0) +
                                              stats.getOrDefault(JobApplication.Status.WITHDRAWN, 0)));
        });
    }
    
    private void loadApplications() {
        if (parentController == null || parentController.currentUser == null) {
            return;
        }
        
        // Show loading
        loadingPane.setVisible(true);
        loadingPane.setManaged(true);
        emptyStatePane.setVisible(false);
        emptyStatePane.setManaged(false);
        
        // Clear existing applications
        applicationsContainer.getChildren().removeIf(node -> node.getStyleClass().contains("job-card"));
        
        // Get applications in background
        Task<List<JobApplication>> task = new Task<>() {
            @Override
            protected List<JobApplication> call() {
                ObjectId employerId = parentController.currentUser.getId();
                
                // Get applications based on filters
                List<JobApplication> applications;
                
                if (selectedJobId != null) {
                    // Filter by specific job
                    applications = applicationService.getApplicationsForJob(selectedJobId);
                } else {
                    // Get all applications for employer's jobs
                    List<Job> jobs = jobService.getJobsByEmployer(employerId);
                    applications = new ArrayList<>();
                    
                    for (Job job : jobs) {
                        applications.addAll(applicationService.getApplicationsForJob(job.getId()));
                    }
                }
                
                // Apply status filter if selected
                if (selectedStatus != null) {
                    applications = applications.stream()
                            .filter(app -> app.getStatus() == selectedStatus)
                            .collect(Collectors.toList());
                }
                
                // Sort by submission date (newest first)
                applications.sort((a1, a2) -> a2.getSubmissionDate().compareTo(a1.getSubmissionDate()));
                
                return applications;
            }
        };
        
        task.setOnSucceeded(event -> {
            List<JobApplication> applications = task.getValue();
            displayApplications(applications);
        });
        
        task.setOnFailed(event -> {
            // Hide loading
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            
            // Show error
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load applications", 
                    "An error occurred while loading applications. Please try again.");
        });
        
        new Thread(task).start();
    }
    
    private void displayApplications(List<JobApplication> applications) {
        Platform.runLater(() -> {
            // Hide loading
            loadingPane.setVisible(false);
            loadingPane.setManaged(false);
            
            if (applications.isEmpty()) {
                // Show empty state
                emptyStatePane.setVisible(true);
                emptyStatePane.setManaged(true);
                return;
            }
            
            // Hide empty state
            emptyStatePane.setVisible(false);
            emptyStatePane.setManaged(false);
            
            // Create application cards
            for (JobApplication application : applications) {
                try {
                    Node applicationCard = createApplicationCard(application);
                    applicationsContainer.getChildren().add(applicationCard);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    private Node createApplicationCard(JobApplication application) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/employer/components/applicationCard.fxml"));
        Node card = loader.load();
        
        // Get job info
        Job job = jobService.getJobById(application.getJobId());
        String jobTitle = job != null ? job.getTitle() : "Unknown Job";
        
        // Set applicant info
        Text applicantNameText = (Text) card.lookup("#applicantNameText");
        Text jobTitleText = (Text) card.lookup("#jobTitleText");
        Text emailText = (Text) card.lookup("#emailText");
        Label statusLabel = (Label) card.lookup("#statusLabel");
        Text applicationDateText = (Text) card.lookup("#applicationDateText");
        Text locationText = (Text) card.lookup("#locationText");
        Text phoneText = (Text) card.lookup("#phoneText");
        
        applicantNameText.setText(application.getFullName());
        jobTitleText.setText(jobTitle);
        emailText.setText(application.getEmail());
        
        // Set status with appropriate styling
        statusLabel.setText(application.getStatusDisplay());
        String statusStyleClass = "status-" + application.getStatus().toString().toLowerCase().replace("_", "-");
        statusLabel.getStyleClass().add(statusStyleClass);
        
        // Format application date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        applicationDateText.setText(application.getSubmissionDate().format(formatter));
        
        // Set location and phone
        locationText.setText(application.getLocation());
        phoneText.setText(application.getPhone());
        
        // Set up resume link
        Hyperlink viewResumeLink = (Hyperlink) card.lookup("#viewResumeLink");
        viewResumeLink.setOnAction(e -> openResume(application.getResumePath()));
        
        // Find the context menu in the card
        ContextMenu contextMenu = null;enuItem");
        for (Node node : card.lookupAll(".context-menu")) {scheduleInterviewMenuItem");
            if (node instanceof ContextMenu) {tem");
                contextMenu = (ContextMenu) node;tem = (MenuItem) card.lookup("#rejectMenuItem");
                break;
            }tion, JobApplication.Status.UNDER_REVIEW));
        }tion, JobApplication.Status.INTERVIEW_SCHEDULED));
        tOnAction(e -> updateApplicationStatus(application, JobApplication.Status.OFFER_EXTENDED));
        // If we can't find the context menu, create onetion(e -> updateApplicationStatus(application, JobApplication.Status.REJECTED));
        if (contextMenu == null) {
            contextMenu = new ContextMenu();
            tton = (Button) card.lookup("#viewDetailsButton");
            MenuItem underReviewMenuItem = new MenuItem("Mark as Under Review");nAction(e -> viewApplicationDetails(application));
            MenuItem scheduleInterviewMenuItem = new MenuItem("Schedule Interview");
            MenuItem makeOfferMenuItem = new MenuItem("Make Offer");
            MenuItem rejectMenuItem = new MenuItem("Reject Application");
            
            contextMenu.getItems().addAll(ate void openResume(String resumePath) {
                    underReviewMenuItem,
                    scheduleInterviewMenuItem,
                    makeOfferMenuItem,
                    rejectMenuItem
            );}
            
            // Attach the context menu to a button in the card
            Button actionsButton = (Button) card.lookup("#actionsButton");
            if (actionsButton != null) {    if (!resumeFile.exists()) {
                actionsButton.setOnMouseClicked(e -> contextMenu.show(actionsButton, e.getScreenX(), e.getScreenY()));Alert(Alert.AlertType.ERROR, "Error", "Resume Not Found", 
            }                   "The resume file could not be found at the specified location.");
                        return;
            underReviewMenuItem.setOnAction(e -> updateApplicationStatus(application, JobApplication.Status.UNDER_REVIEW));
            scheduleInterviewMenuItem.setOnAction(e -> updateApplicationStatus(application, JobApplication.Status.INTERVIEW_SCHEDULED));
            makeOfferMenuItem.setOnAction(e -> updateApplicationStatus(application, JobApplication.Status.OFFER_EXTENDED));
            rejectMenuItem.setOnAction(e -> updateApplicationStatus(application, JobApplication.Status.REJECTED));
        }ception e) {
           showAlert(Alert.AlertType.ERROR, "Error", "Failed to Open Resume", 
        // Set up view details button            "An error occurred while trying to open the resume file: " + e.getMessage());
        Button viewDetailsButton = (Button) card.lookup("#viewDetailsButton");
        viewDetailsButton.setOnAction(e -> viewApplicationDetails(application));
        
        return card;ation.Status newStatus) {
    }
     = applicationService.updateApplicationStatus(application.getId(), newStatus);
    private void openResume(String resumePath) {
        if (resumePath == null || resumePath.isEmpty()) {success) {
            showAlert(Alert.AlertType.ERROR, "Error", "Resume Not Found", 
                    "The resume file could not be found.");
            return;
        }
        
        try {           "Application status has been updated successfully.");
            File resumeFile = new File(resumePath);   } else {
            if (!resumeFile.exists()) {        // Show error message
                showAlert(Alert.AlertType.ERROR, "Error", "Resume Not Found", 
                        "The resume file could not be found at the specified location.");ed while updating the application status. Please try again.");
                return;
            }
            
            // Open the file with the default system applicationn application) {
            java.awt.Desktop.getDesktop().open(resumeFile);
        } catch (Exception e) {// Load application detail view
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to Open Resume",  FXMLLoader(App.class.getResource("/fxml/employer/applicationDetail.fxml"));
                    "An error occurred while trying to open the resume file: " + e.getMessage());
        }
    }et the controller and initialize it
    roller controller = loader.getController();
    private void updateApplicationStatus(JobApplication application, JobApplication.Status newStatus) {
        // Update application status
        boolean success = applicationService.updateApplicationStatus(application.getId(), newStatus);   controller.loadApplication(application.getId());
               
        if (success) {        // Display the detail view
            // Reload applications to reflect changes
            loadApplicationData();arentController.contentArea.getChildren().add(detailView);
            
            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Success", "Status Updated",  "Error", "Navigation Error", 
                    "Application status has been updated successfully.");        "An error occurred while trying to view application details.");
        } else {
            // Show error message
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to Update Status", 
                    "An error occurred while updating the application status. Please try again.");
        }
    }ployerDashboardController) parentController).loadPostJob();
    
    private void viewApplicationDetails(JobApplication application) {
        try { {
            // Load application detail view {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/employer/applicationDetail.fxml"));JobApplication.Status.SUBMITTED;
            Node detailView = loader.load();
            ;
            // Get the controller and initialize it   case "Offer Extended" -> JobApplication.Status.OFFER_EXTENDED;
            ApplicationDetailController controller = loader.getController();       case "Rejected" -> JobApplication.Status.REJECTED;
            controller.setParentController(parentController);        default -> null;
            controller.setApplicationsController(this);;
            controller.loadApplication(application.getId());
            
            // Display the detail viewrivate void showAlert(Alert.AlertType type, String title, String header, String content) {
            parentController.contentArea.getChildren().clear();    Alert alert = new Alert(type);
            parentController.contentArea.getChildren().add(detailView);
        } catch (IOException e) {;
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Navigation Error", 
                    "An error occurred while trying to view application details.");
        }
    }        @FXML    private void onPostNewJobClick() {        ((EmployerDashboardController) parentController).loadPostJob();    }        private JobApplication.Status convertToStatusEnum(String statusStr) {        return switch (statusStr) {            case "Submitted" -> JobApplication.Status.SUBMITTED;            case "Under Review" -> JobApplication.Status.UNDER_REVIEW;            case "Interview Scheduled" -> JobApplication.Status.INTERVIEW_SCHEDULED;            case "Offer Extended" -> JobApplication.Status.OFFER_EXTENDED;            case "Rejected" -> JobApplication.Status.REJECTED;
            default -> null;
        };
    }
    
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
