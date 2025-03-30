package com.jobportal.controllers.jobseeker;

import com.jobportal.App;
import com.jobportal.controllers.common.DashboardController;
import com.jobportal.models.Education;
import com.jobportal.models.Experience;
import com.jobportal.models.User;
import com.jobportal.services.ProfileService;
import com.jobportal.services.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.bson.types.ObjectId;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.layout.Region;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ProfileController implements Initializable {
    
    @FXML
    private Text fullNameText;
    
    @FXML
    private Text emailText;
    
    @FXML
    private ProgressBar profileCompletionBar;
    
    @FXML
    private Text profileCompletionText;
    
    @FXML
    private FontIcon profileIconDefault;
    
    @FXML
    private ImageView profileImageView;
    
    @FXML
    private TextField fullNameField;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private TextField phoneField;
    
    @FXML
    private TextField locationField;
    
    @FXML
    private TextField headlineField;
    
    @FXML
    private TextArea bioField;
    
    @FXML
    private Text errorText;
    
    @FXML
    private Text successText;
    
    @FXML
    private HBox noResumeBox;
    
    @FXML
    private HBox resumeBox;
    
    @FXML
    private Text resumeNameText;
    
    @FXML
    private Text resumeUploadDateText;
    
    @FXML
    private FlowPane skillsContainer;
    
    @FXML
    private TextField newSkillField;
    
    @FXML
    private VBox educationContainer;
    
    @FXML
    private Text noEducationText;
    
    @FXML
    private VBox experienceContainer;
    
    @FXML
    private Text noExperienceText;
    
    private DashboardController parentController;
    private UserService userService;
    private ProfileService profileService;
    
    private User currentUser;
    private Set<String> skills = new HashSet<>();
    private List<Education> educationList = new ArrayList<>();
    private List<Experience> experienceList = new ArrayList<>();
    
    private String resumePath;
    private Date resumeUploadDate;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userService = new UserService();
        profileService = new ProfileService();
    }
    
    public void setParentController(DashboardController controller) {
        this.parentController = controller;
        this.currentUser = parentController.currentUser;
        
        // Load user profile data
        loadProfileData();
    }
    
    private void loadProfileData() {
        if (currentUser == null) return;
        
        // Set basic profile info
        fullNameText.setText(currentUser.getFullName());
        emailText.setText(currentUser.getEmail());
        
        // Set form fields
        fullNameField.setText(currentUser.getFullName());
        usernameField.setText(currentUser.getUsername());
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone());
        locationField.setText(currentUser.getLocation());
        headlineField.setText(currentUser.getHeadline());
        bioField.setText(currentUser.getBio());
        
        // Set profile completion
        int completion = userService.calculateProfileCompletion(currentUser);
        profileCompletionBar.setProgress(completion / 100.0);
        profileCompletionText.setText("Profile " + completion + "% Complete");
        
        // Load profile image if exists
        if (currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
            try {
                File imageFile = new File(currentUser.getProfilePicture());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    profileImageView.setImage(image);
                    profileImageView.setVisible(true);
                    profileIconDefault.setVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Load skills
        if (currentUser.getSkills() != null) {
            skills = new HashSet<>(currentUser.getSkills());
            displaySkills();
        }
        
        // Load resume info
        if (currentUser.getResumePath() != null && !currentUser.getResumePath().isEmpty()) {
            resumePath = currentUser.getResumePath();
            resumeUploadDate = currentUser.getResumeUploadDate();
            displayResumeInfo();
        }
        
        // Load education and experience in background to avoid UI freeze
        new Thread(this::loadEducationAndExperience).start();
    }
    
    private void loadEducationAndExperience() {
        try {
            // Load education
            educationList = profileService.getUserEducation(currentUser.getId());
            
            // Load experience
            experienceList = profileService.getUserExperience(currentUser.getId());
            
            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                displayEducation();
                displayExperience();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    
    private void displayResumeInfo() {
        if (resumePath != null && !resumePath.isEmpty()) {
            File resumeFile = new File(resumePath);
            if (resumeFile.exists()) {
                noResumeBox.setVisible(false);
                noResumeBox.setManaged(false);
                resumeBox.setVisible(true);
                resumeBox.setManaged(true);
                
                resumeNameText.setText(resumeFile.getName());
                
                if (resumeUploadDate != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy");
                    resumeUploadDateText.setText("Uploaded on " + dateFormat.format(resumeUploadDate));
                } else {
                    resumeUploadDateText.setText("Upload date unknown");
                }
            } else {
                // Resume file not found, show upload box
                noResumeBox.setVisible(true);
                noResumeBox.setManaged(true);
                resumeBox.setVisible(false);
                resumeBox.setManaged(false);
            }
        } else {
            // No resume path, show upload box
            noResumeBox.setVisible(true);
            noResumeBox.setManaged(true);
            resumeBox.setVisible(false);
            resumeBox.setManaged(false);
        }
    }
    
    private void displayEducation() {
        educationContainer.getChildren().clear();
        
        if (educationList.isEmpty()) {
            educationContainer.getChildren().add(noEducationText);
            return;
        }
        
        for (Education education : educationList) {
            VBox educationItem = createEducationItem(education);
            educationContainer.getChildren().add(educationItem);
            
            // Add separator except for the last item
            if (educationList.indexOf(education) < educationList.size() - 1) {
                educationContainer.getChildren().add(new Separator());
            }
        }
    }
    
    private VBox createEducationItem(Education education) {
        VBox item = new VBox(5);
        item.getStyleClass().add("profile-item");
        
        // Header with title and action buttons
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Text degreeText = new Text(education.getDegree());
        degreeText.getStyleClass().add("item-title");
        
        Region spacer = new Region();
        spacer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button editButton = new Button();
        editButton.setGraphic(new FontIcon("fas-edit"));
        editButton.getStyleClass().add("item-action-button");
        editButton.setOnAction(e -> editEducation(education));
        
        Button deleteButton = new Button();
        deleteButton.setGraphic(new FontIcon("fas-trash-alt"));
        deleteButton.getStyleClass().add("item-action-button");
        deleteButton.setOnAction(e -> deleteEducation(education));
        
        header.getChildren().addAll(degreeText, spacer, editButton, deleteButton);
        
        // Institution and date
        Text institutionText = new Text(education.getInstitution());
        institutionText.getStyleClass().add("item-subtitle");
        
        String dateRange = formatDateRange(education.getStartDate(), education.getEndDate(), 
                                          education.getCurrentlyStudying());
        Text dateText = new Text(dateRange);
        dateText.getStyleClass().add("item-date");
        
        // Field of study and GPA if available
        VBox details = new VBox(5);
        
        if (education.getFieldOfStudy() != null && !education.getFieldOfStudy().isEmpty()) {
            Text fieldText = new Text("Field of Study: " + education.getFieldOfStudy());
            fieldText.getStyleClass().add("item-detail");
            details.getChildren().add(fieldText);
        }
        
        if (education.getGpa() != null && !education.getGpa().isEmpty()) {
            Text gpaText = new Text("GPA: " + education.getGpa());
            gpaText.getStyleClass().add("item-detail");
            details.getChildren().add(gpaText);
        }
        
        // Description if available
        if (education.getDescription() != null && !education.getDescription().isEmpty()) {
            Text descriptionText = new Text(education.getDescription());
            descriptionText.getStyleClass().add("item-description");
            descriptionText.setWrappingWidth(500);
            details.getChildren().add(descriptionText);
        }
        
        item.getChildren().addAll(header, institutionText, dateText, details);
        return item;
    }
    
    private void displayExperience() {
        experienceContainer.getChildren().clear();
        
        if (experienceList.isEmpty()) {
            experienceContainer.getChildren().add(noExperienceText);
            return;
        }
        
        for (Experience experience : experienceList) {
            VBox experienceItem = createExperienceItem(experience);
            experienceContainer.getChildren().add(experienceItem);
            
            // Add separator except for the last item
            if (experienceList.indexOf(experience) < experienceList.size() - 1) {
                experienceContainer.getChildren().add(new Separator());
            }
        }
    }
    
    private VBox createExperienceItem(Experience experience) {
        VBox item = new VBox(5);
        item.getStyleClass().add("profile-item");
        
        // Header with title and action buttons
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Text positionText = new Text(experience.getPosition());
        positionText.getStyleClass().add("item-title");
        
        Region spacer = new Region();
        spacer.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        Button editButton = new Button();
        editButton.setGraphic(new FontIcon("fas-edit"));
        editButton.getStyleClass().add("item-action-button");
        editButton.setOnAction(e -> editExperience(experience));
        
        Button deleteButton = new Button();
        deleteButton.setGraphic(new FontIcon("fas-trash-alt"));
        deleteButton.getStyleClass().add("item-action-button");
        deleteButton.setOnAction(e -> deleteExperience(experience));
        
        header.getChildren().addAll(positionText, spacer, editButton, deleteButton);
        
        // Company and date
        Text companyText = new Text(experience.getCompany());
        companyText.getStyleClass().add("item-subtitle");
        
        String dateRange = formatDateRange(experience.getStartDate(), experience.getEndDate(), 
                                          experience.getCurrentlyWorking());
        Text dateText = new Text(dateRange);
        dateText.getStyleClass().add("item-date");
        
        // Additional details
        VBox details = new VBox(5);
        
        if (experience.getEmploymentType() != null && !experience.getEmploymentType().isEmpty()) {
            Text typeText = new Text(experience.getEmploymentType());
            typeText.getStyleClass().add("item-detail");
            details.getChildren().add(typeText);
        }
        
        if (experience.getLocation() != null && !experience.getLocation().isEmpty()) {
            Text locationText = new Text(experience.getLocation());
            locationText.getStyleClass().add("item-detail");
            details.getChildren().add(locationText);
        }
        
        // Description if available
        if (experience.getDescription() != null && !experience.getDescription().isEmpty()) {
            Text descriptionText = new Text(experience.getDescription());
            descriptionText.getStyleClass().add("item-description");
            descriptionText.setWrappingWidth(500);
            details.getChildren().add(descriptionText);
        }
        
        item.getChildren().addAll(header, companyText, dateText, details);
        return item;
    }
    
    private String formatDateRange(LocalDate startDate, LocalDate endDate, Boolean current) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        
        if (startDate == null) return "";
        
        StringBuilder dateRange = new StringBuilder();
        dateRange.append(startDate.format(formatter));
        dateRange.append(" - ");
        
        if (Boolean.TRUE.equals(current)) {
            dateRange.append("Present");
        } else if (endDate != null) {
            dateRange.append(endDate.format(formatter));
        } else {
            dateRange.append("Present");
        }
        
        return dateRange.toString();
    }
    
    @FXML
    private void onUploadPhotoClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        File file = fileChooser.showOpenDialog(fullNameField.getScene().getWindow());
        if (file != null) {
            try {
                // Create directory for profile pictures if it doesn't exist
                Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "profile_pictures");
                Files.createDirectories(uploadDir);
                
                // Generate unique filename
                String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getName();
                Path targetPath = uploadDir.resolve(uniqueFileName);
                
                // Copy file to target location
                Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Update user's profile picture path
                currentUser.setProfilePicture(targetPath.toString());
                userService.updateUser(currentUser);
                
                // Display the new profile picture
                Image image = new Image(targetPath.toUri().toString());
                profileImageView.setImage(image);
                profileImageView.setVisible(true);
                profileIconDefault.setVisible(false);
                
                showSuccess("Profile picture updated successfully");
            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to upload profile picture. Please try again.");
            }
        }
    }
    
    @FXML
    private void onSaveProfileClick() {
        if (!validateProfileForm()) {
            return;
        }
        
        try {
            // Update user object with form values
            currentUser.setFullName(fullNameField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setPhone(phoneField.getText().trim());
            currentUser.setLocation(locationField.getText().trim());
            currentUser.setHeadline(headlineField.getText().trim());
            currentUser.setBio(bioField.getText().trim());
            currentUser.setSkills(new ArrayList<>(skills));
            
            // Save to database
            boolean success = userService.updateUser(currentUser);
            
            if (success) {
                // Update profile completion
                int completion = userService.calculateProfileCompletion(currentUser);
                profileCompletionBar.setProgress(completion / 100.0);
                profileCompletionText.setText("Profile " + completion + "% Complete");
                
                // Update header text
                fullNameText.setText(currentUser.getFullName());
                emailText.setText(currentUser.getEmail());
                
                showSuccess("Profile updated successfully");
            } else {
                showError("Failed to update profile. Please try again.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("An error occurred: " + e.getMessage());
        }
    }
    
    private boolean validateProfileForm() {
        // Clear previous messages
        hideError();
        hideSuccess();
        
        // Validate required fields
        if (fullNameField.getText().trim().isEmpty()) {
            showError("Full name is required");
            return false;
        }
        
        if (emailField.getText().trim().isEmpty()) {
            showError("Email is required");
            return false;
        }
        
        // Validate email format
        if (!emailField.getText().trim().matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            showError("Please enter a valid email address");
            return false;
        }
        
        return true;
    }
    
    @FXML
    private void onUploadResumeClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Resume");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Document Files", "*.pdf", "*.doc", "*.docx")
        );
        
        File file = fileChooser.showOpenDialog(fullNameField.getScene().getWindow());
        if (file != null) {
            try {
                // Create directory for resumes if it doesn't exist
                Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "resumes");
                Files.createDirectories(uploadDir);
                
                // Generate unique filename
                String uniqueFileName = UUID.randomUUID().toString() + "_" + file.getName();
                Path targetPath = uploadDir.resolve(uniqueFileName);
                
                // Copy file to target location
                Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Update resumePath and upload date
                resumePath = targetPath.toString();
                resumeUploadDate = new Date();
                
                // Update user's resume info
                currentUser.setResumePath(resumePath);
                currentUser.setResumeUploadDate(resumeUploadDate);
                userService.updateUser(currentUser);
                
                // Display resume info
                displayResumeInfo();
                
                showSuccess("Resume uploaded successfully");
            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to upload resume. Please try again.");
            }
        }
    }
    
    @FXML
    private void onViewResumeClick() {
        if (resumePath == null || resumePath.isEmpty()) {
            showError("Resume file not found");
            return;
        }
        
        try {
            File resumeFile = new File(resumePath);
            if (!resumeFile.exists()) {
                showError("Resume file not found at the specified location");
                return;
            }
            
            // Open the file with the default system application
            java.awt.Desktop.getDesktop().open(resumeFile);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open resume file: " + e.getMessage());
        }
    }
    
    @FXML
    private void onReplaceResumeClick() {
        onUploadResumeClick();
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
    
    @FXML
    private void onAddEducationClick() {
        try {
            showEducationDialog(null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open education dialog");
        }
    }
    
    private void editEducation(Education education) {
        try {
            showEducationDialog(education);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open education dialog");
        }
    }
    
    private void deleteEducation(Education education) {
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Education");
        alert.setContentText("Are you sure you want to delete this education entry?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = profileService.deleteEducation(education.getId());
                if (success) {
                    educationList.remove(education);
                    displayEducation();
                    showSuccess("Education entry deleted successfully");
                } else {
                    showError("Failed to delete education entry");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("An error occurred: " + e.getMessage());
            }
        }
    }
    
    private void showEducationDialog(Education education) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/components/educationDialog.fxml"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(education == null ? "Add Education" : "Edit Education");
        
        dialog.getDialogPane().setContent(loader.load());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        
        // Get dialog fields
        Text dialogTitle = (Text) dialog.getDialogPane().lookup("#dialogTitle");
        TextField institutionField = (TextField) dialog.getDialogPane().lookup("#institutionField");
        TextField degreeField = (TextField) dialog.getDialogPane().lookup("#degreeField");
        TextField fieldOfStudyField = (TextField) dialog.getDialogPane().lookup("#fieldOfStudyField");
        DatePicker startDatePicker = (DatePicker) dialog.getDialogPane().lookup("#startDatePicker");
        DatePicker endDatePicker = (DatePicker) dialog.getDialogPane().lookup("#endDatePicker");
        CheckBox currentlyStudyingCheckbox = (CheckBox) dialog.getDialogPane().lookup("#currentlyStudyingCheckbox");
        TextField gpaField = (TextField) dialog.getDialogPane().lookup("#gpaField");
        TextArea descriptionField = (TextArea) dialog.getDialogPane().lookup("#descriptionField");
        Text errorText = (Text) dialog.getDialogPane().lookup("#errorText");
        
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        
        // Set dialog title
        dialogTitle.setText(education == null ? "Add Education" : "Edit Education");
        
        // Populate fields if editing
        if (education != null) {
            institutionField.setText(education.getInstitution());
            degreeField.setText(education.getDegree());
            fieldOfStudyField.setText(education.getFieldOfStudy());
            startDatePicker.setValue(education.getStartDate());
            
            if (Boolean.TRUE.equals(education.getCurrentlyStudying())) {
                currentlyStudyingCheckbox.setSelected(true);
                endDatePicker.setDisable(true);
            } else {
                endDatePicker.setValue(education.getEndDate());
            }
            
            gpaField.setText(education.getGpa());
            descriptionField.setText(education.getDescription());
        }
        
        // Set up currently studying checkbox behavior
        currentlyStudyingCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            endDatePicker.setDisable(newValue);
            if (newValue) {
                endDatePicker.setValue(null);
            }
        });
        
        // Set up validation
        saveButton.setOnAction(e -> {
            try {
                // Validate required fields
                if (institutionField.getText().trim().isEmpty() || 
                    degreeField.getText().trim().isEmpty() || 
                    startDatePicker.getValue() == null) {
                    errorText.setText("Please fill in all required fields");
                    errorText.setVisible(true);
                    e.consume(); // Prevent dialog from closing
                    return;
                }
                
                // Create or update education object
                Education newEducation = education == null ? new Education() : education;
                newEducation.setUserId(currentUser.getId());
                newEducation.setInstitution(institutionField.getText().trim());
                newEducation.setDegree(degreeField.getText().trim());
                newEducation.setFieldOfStudy(fieldOfStudyField.getText().trim());
                newEducation.setStartDate(startDatePicker.getValue());
                newEducation.setCurrentlyStudying(currentlyStudyingCheckbox.isSelected());
                
                if (!currentlyStudyingCheckbox.isSelected()) {
                    newEducation.setEndDate(endDatePicker.getValue());
                } else {
                    newEducation.setEndDate(null);
                }
                
                newEducation.setGpa(gpaField.getText().trim());
                newEducation.setDescription(descriptionField.getText().trim());
                
                // Save to database
                if (education == null) {
                    profileService.addEducation(newEducation);
                    educationList.add(newEducation);
                } else {
                    profileService.updateEducation(newEducation);
                    // Update in list
                    int index = educationList.indexOf(education);
                    if (index >= 0) {
                        educationList.set(index, newEducation);
                    }
                }
                
                // Update UI
                displayEducation();
                
                // Show success message
                showSuccess(education == null ? "Education added successfully" : "Education updated successfully");
                
                dialog.close();
            } catch (Exception ex) {
                errorText.setText("Error: " + ex.getMessage());
                errorText.setVisible(true);
                e.consume(); // Prevent dialog from closing
            }
        });
        
        dialog.showAndWait();
    }
    
    @FXML
    private void onAddExperienceClick() {
        try {
            showExperienceDialog(null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open experience dialog");
        }
    }
    
    private void editExperience(Experience experience) {
        try {
            showExperienceDialog(experience);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open experience dialog");
        }
    }
    
    private void deleteExperience(Experience experience) {
        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Experience");
        alert.setContentText("Are you sure you want to delete this experience entry?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = profileService.deleteExperience(experience.getId());
                if (success) {
                    experienceList.remove(experience);
                    displayExperience();
                    showSuccess("Experience entry deleted successfully");
                } else {
                    showError("Failed to delete experience entry");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("An error occurred: " + e.getMessage());
            }
        }
    }
    
    private void showExperienceDialog(Experience experience) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/jobseeker/components/experienceDialog.fxml"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(experience == null ? "Add Work Experience" : "Edit Work Experience");
        
        dialog.getDialogPane().setContent(loader.load());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        
        // Get dialog fields
        Text dialogTitle = (Text) dialog.getDialogPane().lookup("#dialogTitle");
        TextField companyField = (TextField) dialog.getDialogPane().lookup("#companyField");
        TextField positionField = (TextField) dialog.getDialogPane().lookup("#positionField");
        TextField locationField = (TextField) dialog.getDialogPane().lookup("#locationField");
        ComboBox<String> employmentTypeComboBox = (ComboBox<String>) dialog.getDialogPane().lookup("#employmentTypeComboBox");
        DatePicker startDatePicker = (DatePicker) dialog.getDialogPane().lookup("#startDatePicker");
        DatePicker endDatePicker = (DatePicker) dialog.getDialogPane().lookup("#endDatePicker");
        CheckBox currentlyWorkingCheckbox = (CheckBox) dialog.getDialogPane().lookup("#currentlyWorkingCheckbox");
        TextArea descriptionField = (TextArea) dialog.getDialogPane().lookup("#descriptionField");
        Text errorText = (Text) dialog.getDialogPane().lookup("#errorText");
        
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        
        // Set dialog title
        dialogTitle.setText(experience == null ? "Add Work Experience" : "Edit Work Experience");
        
        // Populate fields if editing
        if (experience != null) {
            companyField.setText(experience.getCompany());
            positionField.setText(experience.getPosition());
            locationField.setText(experience.getLocation());
            employmentTypeComboBox.setValue(experience.getEmploymentType());
            startDatePicker.setValue(experience.getStartDate());
            
            if (Boolean.TRUE.equals(experience.getCurrentlyWorking())) {
                currentlyWorkingCheckbox.setSelected(true);
                endDatePicker.setDisable(true);
            } else {
                endDatePicker.setValue(experience.getEndDate());
            }
            
            descriptionField.setText(experience.getDescription());
        }
        
        // Set up currently working checkbox behavior
        currentlyWorkingCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            endDatePicker.setDisable(newValue);
            if (newValue) {
                endDatePicker.setValue(null);
            }
        });
        
        // Set up validation
        saveButton.setOnAction(e -> {
            try {
                // Validate required fields
                if (companyField.getText().trim().isEmpty() || 
                    positionField.getText().trim().isEmpty() || 
                    startDatePicker.getValue() == null) {
                    errorText.setText("Please fill in all required fields");
                    errorText.setVisible(true);
                    e.consume(); // Prevent dialog from closing
                    return;
                }
                
                // Create or update experience object
                Experience newExperience = experience == null ? new Experience() : experience;
                newExperience.setUserId(currentUser.getId());
                newExperience.setCompany(companyField.getText().trim());
                newExperience.setPosition(positionField.getText().trim());
                newExperience.setLocation(locationField.getText().trim());
                newExperience.setEmploymentType(employmentTypeComboBox.getValue());
                newExperience.setStartDate(startDatePicker.getValue());
                newExperience.setCurrentlyWorking(currentlyWorkingCheckbox.isSelected());
                
                if (!currentlyWorkingCheckbox.isSelected()) {
                    newExperience.setEndDate(endDatePicker.getValue());
                } else {
                    newExperience.setEndDate(null);
                }
                
                newExperience.setDescription(descriptionField.getText().trim());
                
                // Save to database
                if (experience == null) {
                    profileService.addExperience(newExperience);
                    experienceList.add(newExperience);
                } else {
                    profileService.updateExperience(newExperience);
                    // Update in list
                    int index = experienceList.indexOf(experience);
                    if (index >= 0) {
                        experienceList.set(index, newExperience);
                    }
                }
                
                // Update UI
                displayExperience();
                
                // Show success message
                showSuccess(experience == null ? "Experience added successfully" : "Experience updated successfully");
                
                dialog.close();
            } catch (Exception ex) {
                errorText.setText("Error: " + ex.getMessage());
                errorText.setVisible(true);
                e.consume(); // Prevent dialog from closing
            }
        });
        
        dialog.showAndWait();
    }
    
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisible(true);
        successText.setVisible(false);
    }
    
    private void hideError() {
        errorText.setVisible(false);
    }
    
    private void showSuccess(String message) {
        successText.setText(message);
        successText.setVisible(true);
        errorText.setVisible(false);
    }
    
    private void hideSuccess() {
        successText.setVisible(false);
    }
}
