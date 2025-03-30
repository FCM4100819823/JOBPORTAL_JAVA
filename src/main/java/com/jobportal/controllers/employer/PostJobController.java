package com.jobportal.controllers.employer;

import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Job;
import com.jobportal.services.JobService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class PostJobController implements Initializable {
    
    @FXML
    private TextField jobTitleField;
    
    @FXML
    private TextField companyField;
    
    @FXML
    private TextField locationField;
    
    @FXML
    private ComboBox<String> employmentTypeComboBox;
    
    @FXML
    private ComboBox<String> experienceLevelComboBox;
    
    @FXML
    private ComboBox<String> currencyComboBox;
    
    @FXML
    private TextField minSalaryField;
    
    @FXML
    private TextField maxSalaryField;
    
    @FXML
    private TextField positionsField;
    
    @FXML
    private TextArea descriptionField;
    
    @FXML
    private FlowPane skillsContainer;
    
    @FXML
    private TextField newSkillField;
    
    @FXML
    private TextArea applicationInstructionsField;
    
    @FXML
    private DatePicker deadlinePicker;
    
    @FXML
    private Text errorText;
    
    @FXML
    private Text successText;
    
    @FXML
    private Button clearButton;
    
    @FXML
    private Button previewButton;
    
    @FXML
    private Button submitButton;
    
    private DashboardController parentController;
    private JobService jobService;
    private Set<String> skills = new HashSet<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobService = new JobService();
        
        // Set default values
        currencyComboBox.setValue("$");
        
        // Pre-fill company name if available
        if (parentController != null && parentController.currentUser != null) {
            companyField.setText(parentController.currentUser.getFullName());
        }
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        
        // Pre-fill company name if available
        if (parentController.currentUser != null) {
            companyField.setText(parentController.currentUser.getFullName());
        }
    }
    
    @FXML
    private void onAddSkillClick() {
        String skill = newSkillField.getText().trim();
        
        if (skill.isEmpty()) {
            return;
        }
        
        // Add skill to set (handles duplicates)
        skills.add(skill);
        
        // Clear the input field
        newSkillField.clear();
        
        // Update UI
        displaySkills();
    }
    
    private void displaySkills() {
        skillsContainer.getChildren().clear();
        
        for (String skill : skills) {
            // Create a chip for each skill
            HBox skillChip = createSkillChip(skill);
            skillsContainer.getChildren().add(skillChip);
        }
    }
    
    private HBox createSkillChip(String skill) {
        HBox chip = new HBox(5);
        chip.getStyleClass().add("skill-chip");
        chip.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
        
        Label label = new Label(skill);
        Button removeButton = new Button();
        removeButton.setGraphic(new FontIcon("fas-times"));
        removeButton.getStyleClass().add("chip-remove-button");
        removeButton.setOnAction(e -> {
            skills.remove(skill);
            displaySkills();
        });
        
        chip.getChildren().addAll(label, removeButton);
        return chip;
    }
    
    @FXML
    private void onClearButtonClick() {
        // Clear form fields
        jobTitleField.clear();
        locationField.clear();
        employmentTypeComboBox.getSelectionModel().clearSelection();
        experienceLevelComboBox.getSelectionModel().clearSelection();
        minSalaryField.clear();
        maxSalaryField.clear();
        positionsField.clear();
        descriptionField.clear();
        skills.clear();
        displaySkills();
        applicationInstructionsField.clear();
        deadlinePicker.setValue(null);
        
        // Clear error/success messages
        errorText.setVisible(false);
        successText.setVisible(false);
    }
    
    @FXML
    private void onPreviewButtonClick() {
        if (!validateForm()) {
            return;
        }
        
        Job job = createJobFromForm();
        
        // In a real application, you would show a preview dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Job Preview");
        alert.setHeaderText(job.getTitle() + " at " + job.getCompany());
        alert.setContentText("This is a preview of your job posting.\n\n" + 
                "Location: " + job.getLocation() + "\n" +
                "Type: " + job.getEmploymentType() + "\n" +
                "Experience: " + job.getExperienceLevel() + "\n\n" +
                "Description: " + job.getDescription());
        alert.showAndWait();
    }
    
    @FXML
    private void onSubmitButtonClick() {
        if (!validateForm()) {
            return;
        }
        
        try {
            Job job = createJobFromForm();
            
            // Set the employer ID
            job.setEmployerId(parentController.currentUser.getId());
            
            // Set the post date and active status
            job.setPostDate(LocalDateTime.now());
            job.setActive(true);
            
            // Save job to database
            Job createdJob = jobService.createJob(job);
            
            if (createdJob != null && createdJob.getId() != null) {
                // Show success message
                showSuccess("Job posted successfully!");
                
                // Clear form after successful submission
                onClearButtonClick();
            } else {
                showError("Failed to post job. Please try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("An error occurred: " + e.getMessage());
        }
    }
    
    private boolean validateForm() {
        // Clear previous messages
        errorText.setVisible(false);
        successText.setVisible(false);
        
        // Validate required fields
        if (jobTitleField.getText().trim().isEmpty()) {
            showError("Job title is required");
            return false;
        }
        
        if (companyField.getText().trim().isEmpty()) {
            showError("Company name is required");
            return false;
        }
        
        if (locationField.getText().trim().isEmpty()) {
            showError("Location is required");
            return false;
        }
        
        if (employmentTypeComboBox.getValue() == null) {
            showError("Employment type is required");
            return false;
        }
        
        if (experienceLevelComboBox.getValue() == null) {
            showError("Experience level is required");
            return false;
        }
        
        if (descriptionField.getText().trim().isEmpty()) {
            showError("Job description is required");
            return false;
        }
        
        // Validate salary range if provided
        if (!minSalaryField.getText().trim().isEmpty() || !maxSalaryField.getText().trim().isEmpty()) {
            try {
                if (!minSalaryField.getText().trim().isEmpty()) {
                    Double.parseDouble(minSalaryField.getText().trim());
                }
                
                if (!maxSalaryField.getText().trim().isEmpty()) {
                    Double.parseDouble(maxSalaryField.getText().trim());
                }
                
                // Check that min <= max if both are provided
                if (!minSalaryField.getText().trim().isEmpty() && !maxSalaryField.getText().trim().isEmpty()) {
                    double min = Double.parseDouble(minSalaryField.getText().trim());
                    double max = Double.parseDouble(maxSalaryField.getText().trim());
                    
                    if (min > max) {
                        showError("Minimum salary cannot be greater than maximum salary");
                        return false;
                    }
                }
            } catch (NumberFormatException e) {
                showError("Please enter valid numbers for salary range");
                return false;
            }
        }
        
        // Validate number of positions if provided
        if (!positionsField.getText().trim().isEmpty()) {
            try {
                int positions = Integer.parseInt(positionsField.getText().trim());
                
                if (positions <= 0) {
                    showError("Number of positions must be greater than 0");
                    return false;
                }
            } catch (NumberFormatException e) {
                showError("Please enter a valid number for positions");
                return false;
            }
        }
        
        return true;
    }
    
    private Job createJobFromForm() {
        Job job = new Job();
        
        // Set job properties
        job.setTitle(jobTitleField.getText().trim());
        job.setCompany(companyField.getText().trim());
        job.setLocation(locationField.getText().trim());
        job.setEmploymentType(employmentTypeComboBox.getValue());
        job.setExperienceLevel(experienceLevelComboBox.getValue());
        job.setDescription(descriptionField.getText().trim());
        
        // Set salary range if provided
        if (!minSalaryField.getText().trim().isEmpty()) {
            job.setSalaryMin(Double.parseDouble(minSalaryField.getText().trim()));
        }
        
        if (!maxSalaryField.getText().trim().isEmpty()) {
            job.setSalaryMax(Double.parseDouble(maxSalaryField.getText().trim()));
        }
        
        // Set currency
        job.setCurrency(currencyComboBox.getValue());
        
        // Set number of positions if provided
        if (!positionsField.getText().trim().isEmpty()) {
            job.setNumberOfPositions(Integer.parseInt(positionsField.getText().trim()));
        }
        
        // Set skills
        job.setSkills(new ArrayList<>(skills));
        
        // Set application instructions if provided
        if (!applicationInstructionsField.getText().trim().isEmpty()) {
            job.setApplicationInstructions(applicationInstructionsField.getText().trim());
        }
        
        // Set application deadline if provided
        if (deadlinePicker.getValue() != null) {
            LocalDate deadline = deadlinePicker.getValue();
            job.setApplicationDeadline(deadline.atStartOfDay());
        }
        
        return job;
    }
    
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisible(true);
        successText.setVisible(false);
    }
    
    private void showSuccess(String message) {
        successText.setText(message);
        successText.setVisible(true);
        errorText.setVisible(false);
    }
}
