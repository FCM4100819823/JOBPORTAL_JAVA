package com.jobportal.controllers.jobseeker;

import com.jobportal.App;
import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.services.ApplicationService;
import com.jobportal.services.JobService;
import com.jobportal.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.bson.types.ObjectId;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;

public class JobDetailController implements Initializable {

    @FXML
    private Text jobTitleText;

    @FXML
    private Text companyText;

    @FXML
    private Text locationText;

    @FXML
    private Button saveJobButton;

    @FXML
    private FontIcon saveIcon;

    @FXML
    private Label saveJobLabel;

    @FXML
    private Button applyButton;

    @FXML
    private Text jobTypeText;

    @FXML
    private Text salaryText;

    @FXML
    private Text experienceLevelText;

    @FXML
    private Text postedDateText;

    @FXML
    private TextFlow descriptionTextFlow;

    @FXML
    private FlowPane skillsContainer;

    @FXML
    private Text companyNameText;

    @FXML
    private Text companyLocationText;

    @FXML
    private Text companyDescriptionText;

    @FXML
    private Button applyNowButton;

    @FXML
    private VBox similarJobsContainer;

    @FXML
    private Label noSimilarJobsLabel;

    private JobService jobService;
    private UserService userService;
    private ApplicationService applicationService;
    private DashboardController parentController;
    private Job currentJob;
    private String previousScreen;
    private Parent root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobService = new JobService();
        userService = new UserService();
        applicationService = new ApplicationService();
    }

    public void setParentController(DashboardController controller) {
        this.parentController = controller;
    }

    public void loadJob(String jobId, String previousScreen) {
        this.previousScreen = previousScreen;

        try {
            ObjectId objectId = new ObjectId(jobId);
            currentJob = jobService.getJobById(objectId);

            if (currentJob != null) {
                displayJobDetails();

                // Check if user has already applied
                if (parentController != null && parentController.currentUser != null) {
                    boolean hasApplied = applicationService.hasApplied(
                            currentJob.getId(),
                            parentController.currentUser.getId()
                    );

                    if (hasApplied) {
                        applyButton.setText("Applied");
                        applyButton.setDisable(true);
                        applyNowButton.setText("Applied");
                        applyNowButton.setDisable(true);
                    }

                    // Check if job is saved
                    boolean isSaved = userService.isJobSaved(
                            parentController.currentUser.getId(),
                            currentJob.getId()
                    );

                    if (isSaved) {
                        saveIcon.setIconLiteral("fas-bookmark");
                        saveJobLabel.setText("Saved");
                    }
                }

                // Load similar jobs
                loadSimilarJobs();

                // Set up save button
                saveJobButton.setOnAction(e -> toggleSaveJob());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Show error in UI
        }
    }

    private void displayJobDetails() {
        jobTitleText.setText(currentJob.getTitle());
        companyText.setText(currentJob.getCompany());
        locationText.setText(currentJob.getLocation());

        // Job type
        jobTypeText.setText(currentJob.getEmploymentType());

        // Salary
        if (currentJob.getSalaryMin() != null) {
            String salaryDisplay = formatSalaryRange(
                    currentJob.getSalaryMin(),
                    currentJob.getSalaryMax(),
                    currentJob.getCurrency()
            );
            salaryText.setText(salaryDisplay);
        } else {
            salaryText.setText("Not specified");
        }

        // Experience level
        experienceLevelText.setText(currentJob.getExperienceLevel());

        // Posted date
        String postedDate = formatTimeAgo(currentJob.getPostDate());
        postedDateText.setText(postedDate);

        // Description
        Text descriptionText = new Text(currentJob.getDescription());
        descriptionTextFlow.getChildren().clear();
        descriptionTextFlow.getChildren().add(descriptionText);

        // Skills
        skillsContainer.getChildren().clear();
        if (currentJob.getSkills() != null && !currentJob.getSkills().isEmpty()) {
            for (String skill : currentJob.getSkills()) {
                Label skillTag = new Label(skill);
                skillTag.getStyleClass().add("job-tag");
                skillsContainer.getChildren().add(skillTag);
            }
        }

        // Company info
        companyNameText.setText(currentJob.getCompany());
        companyLocationText.setText(currentJob.getLocation());

        // Placeholder for company description - in a real app, you would have a Company model
        companyDescriptionText.setText("Description not available for " + currentJob.getCompany());
    }

    private void loadSimilarJobs() {
        // In a real app, you would have a more sophisticated algorithm
        // For now, just get jobs with similar title or skills
        List<Job> similarJobs = jobService.getSimilarJobs(currentJob, 3);

        similarJobsContainer.getChildren().clear();

        if (similarJobs.isEmpty()) {
            noSimilarJobsLabel.setVisible(true);
        } else {
            noSimilarJobsLabel.setVisible(false);

            for (Job job : similarJobs) {
                if (!job.getId().equals(currentJob.getId())) {
                    try {
                        HBox jobCard = createSimplifiedJobCard(job);
                        similarJobsContainer.getChildren().add(jobCard);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private HBox createSimplifiedJobCard(Job job) throws IOException {
        // Create a simplified job card for similar jobs section
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/components/simplifiedJobCard.fxml"));
        HBox jobCard = loader.load();

        // Set job title
        Text jobTitleText = (Text) jobCard.lookup("#jobTitleText");
        jobTitleText.setText(job.getTitle());

        // Set company name
        Text companyText = (Text) jobCard.lookup("#companyText");
        companyText.setText(job.getCompany());

        // Set location
        Text locationText = (Text) jobCard.lookup("#locationText");
        locationText.setText(job.getLocation());

        // Set view details action
        Button viewDetailsButton = (Button) jobCard.lookup("#viewDetailsButton");
        viewDetailsButton.setOnAction(e -> viewJobDetails(job.getId().toString()));

        return jobCard;
    }

    private void toggleSaveJob() {
        if (parentController == null || parentController.currentUser == null || currentJob == null) {
            return;
        }

        ObjectId userId = parentController.currentUser.getId();
        boolean isSaved = saveIcon.getIconLiteral().equals("fas-bookmark");

        if (isSaved) {
            // Unsave the job
            if (userService.unsaveJob(userId, currentJob.getId())) {
                saveIcon.setIconLiteral("far-bookmark");
                saveJobLabel.setText("Save Job");
            }
        } else {
            // Save the job
            if (userService.saveJob(userId, currentJob.getId())) {
                saveIcon.setIconLiteral("fas-bookmark");
                saveJobLabel.setText("Saved");
            }
        }
    }

    @FXML
    private void onApplyButtonClick() {
        if (currentJob != null) {
            try {
                // Navigate to application form
                FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/jobApplication.fxml"));
                Node applicationForm = loader.load();

                // Get the controller and initialize it with the job
                JobApplicationController controller = loader.getController();
                controller.setParentController(parentController);
                controller.initializeForJob(currentJob, this);

                // Switch to the application form
                parentController.contentArea.getChildren().clear();
                parentController.contentArea.getChildren().add(applicationForm);
            } catch (IOException e) {
                e.printStackTrace();
                // Show error in UI
            }
        }
    }

    @FXML
    private void onBackButtonClick() {
        try {
            // Go back to previous screen
            if ("browse".equals(previousScreen)) {
                ((JobSeekerDashboardController) parentController).loadBrowseJobs();
            } else {
                ((JobSeekerDashboardController) parentController).loadDashboardHome();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Show error in UI
        }
    }

    private void viewJobDetails(String jobId) {
        // This method is for viewing details of similar jobs
        loadJob(jobId, previousScreen);
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

    private String formatTimeAgo(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "recently";
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(dateTime, now);

        if (days == 0) {
            long hours = ChronoUnit.HOURS.between(dateTime, now);
            if (hours == 0) {
                long minutes = ChronoUnit.MINUTES.between(dateTime, now);
                return minutes + " minute" + (minutes != 1 ? "s" : "") + " ago";
            }
            return hours + " hour" + (hours != 1 ? "s" : "") + " ago";
        } else if (days < 30) {
            return days + " day" + (days != 1 ? "s" : "") + " ago";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            return "on " + dateTime.format(formatter);
        }
    }

    public Parent getRoot() {
        return root;
    }

    public void setRoot(Parent root) {
        this.root = root;
    }
}
