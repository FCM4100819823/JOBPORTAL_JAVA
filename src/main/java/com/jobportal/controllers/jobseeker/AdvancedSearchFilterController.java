package com.jobportal.controllers.jobseeker;

import com.jobportal.services.JobService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

public class AdvancedSearchFilterController implements Initializable {
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private TextField locationField;
    
    @FXML
    private CheckBox remoteCheckbox;
    
    @FXML
    private ComboBox<String> jobTypeComboBox;
    
    @FXML
    private ComboBox<String> experienceLevelComboBox;
    
    @FXML
    private Slider salarySlider;
    
    @FXML
    private Text salaryRangeText;
    
    @FXML
    private RadioButton anyTimeRadio;
    
    @FXML
    private RadioButton pastDayRadio;
    
    @FXML
    private RadioButton pastWeekRadio;
    
    @FXML
    private RadioButton pastMonthRadio;
    
    @FXML
    private TextField skillField;
    
    @FXML
    private Button addSkillButton;
    
    @FXML
    private FlowPane skillsContainer;
    
    @FXML
    private Button clearFiltersButton;
    
    @FXML
    private Button applyFiltersButton;
    
    private JobService jobService;
    private Set<String> selectedSkills = new HashSet<>();
    
    // Property that will hold our filter criteria for binding to parent controller
    private final ObjectProperty<JobSearchCriteria> searchCriteria = new SimpleObjectProperty<>(new JobSearchCriteria());
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        jobService = new JobService();
        
        // Initialize filter options
        initializeFilterOptions();
        
        // Set up event handlers
        setupEventHandlers();
        
        // Set up property listeners
        setupPropertyListeners();
    }
    
    private void initializeFilterOptions() {
        // Populate job types
        jobTypeComboBox.getItems().addAll(jobService.getAllEmploymentTypes());
        
        // Populate experience levels
        experienceLevelComboBox.getItems().addAll(jobService.getAllExperienceLevels());
        
        // Configure salary slider
        setupSalarySlider();
        
        salarySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int value = newVal.intValue();
            salaryRangeText.setText("$" + formatSalary(value) + "+");
        });
    }
    
    private void setupSalarySlider() {
        // Create custom string converter for salary values
        salarySlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                if (value == null) {
                    return "";
                }
                return "$" + String.format("%,.0f", value);
            }
            
            @Override
            public Double fromString(String string) {
                if (string == null || string.isEmpty()) {
                    return null;
                }
                try {
                    // Remove currency symbol and commas
                    string = string.replace("$", "").replaceAll("[,]", "");
                    return Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });
    }
    
    private void setupEventHandlers() {
        // Search button
        searchButton.setOnAction(e -> updateSearchCriteria());
        
        // Add skill button
        addSkillButton.setOnAction(e -> addSkill());
        skillField.setOnAction(e -> addSkill());
        
        // Clear filters button
        clearFiltersButton.setOnAction(e -> clearAllFilters());
        
        // Apply filters button
        applyFiltersButton.setOnAction(e -> updateSearchCriteria());
    }
    
    private void setupPropertyListeners() {
        // Update search criteria when Enter is pressed in search field
        searchField.setOnAction(e -> updateSearchCriteria());
    }
    
    private void addSkill() {
        String skill = skillField.getText().trim();
        if (!skill.isEmpty()) {
            selectedSkills.add(skill);
            updateSkillsContainer();
            skillField.clear();
        }
    }
    
    private void updateSkillsContainer() {
        skillsContainer.getChildren().clear();
        
        for (String skill : selectedSkills) {
            HBox skillChip = createSkillChip(skill);
            skillsContainer.getChildren().add(skillChip);
        }
    }
    
    private HBox createSkillChip(String skill) {
        HBox chip = new HBox(5);
        chip.getStyleClass().add("filter-chip");
        chip.setPadding(new javafx.geometry.Insets(3, 8, 3, 8));
        
        Label label = new Label(skill);
        Button removeButton = new Button();
        removeButton.getStyleClass().add("chip-remove-button");
        removeButton.setGraphic(new FontIcon("fas-times"));
        removeButton.setOnAction(e -> {
            selectedSkills.remove(skill);
            updateSkillsContainer();
        });
        
        chip.getChildren().addAll(label, removeButton);
        return chip;
    }
    
    private void clearAllFilters() {
        searchField.clear();
        locationField.clear();
        remoteCheckbox.setSelected(false);
        jobTypeComboBox.getSelectionModel().clearSelection();
        experienceLevelComboBox.getSelectionModel().clearSelection();
        salarySlider.setValue(0);
        anyTimeRadio.setSelected(true);
        selectedSkills.clear();
        updateSkillsContainer();
        
        // Update search criteria with cleared filters
        updateSearchCriteria();
    }
    
    private void updateSearchCriteria() {
        JobSearchCriteria criteria = new JobSearchCriteria();
        
        // Set keyword
        criteria.setKeyword(searchField.getText().trim());
        
        // Set location
        criteria.setLocation(locationField.getText().trim());
        criteria.setRemoteOnly(remoteCheckbox.isSelected());
        
        // Set job type
        criteria.setJobType(jobTypeComboBox.getValue());
        
        // Set experience level
        criteria.setExperienceLevel(experienceLevelComboBox.getValue());
        
        // Set minimum salary
        if (salarySlider.getValue() > 0) {
            criteria.setMinSalary(salarySlider.getValue());
        }
        
        // Set date posted filter
        LocalDateTime now = LocalDateTime.now();
        if (pastDayRadio.isSelected()) {
            criteria.setPostDateMin(now.minusDays(1));
        } else if (pastWeekRadio.isSelected()) {
            criteria.setPostDateMin(now.minusWeeks(1));
        } else if (pastMonthRadio.isSelected()) {
            criteria.setPostDateMin(now.minusMonths(1));
        }
        
        // Set skills
        if (!selectedSkills.isEmpty()) {
            criteria.setSkills(new ArrayList<>(selectedSkills));
        }
        
        // Update the property with the new criteria
        searchCriteria.set(criteria);
    }
    
    public ObjectProperty<JobSearchCriteria> searchCriteriaProperty() {
        return searchCriteria;
    }
    
    private String formatSalary(int salary) {
        return String.format("%,d", salary);
    }
    
    // Search criteria class to hold all filter options
    public static class JobSearchCriteria {
        private String keyword;
        private String location;
        private boolean remoteOnly;
        private String jobType;
        private String experienceLevel;
        private Double minSalary;
        private LocalDateTime postDateMin;
        private List<String> skills;
        
        public JobSearchCriteria() {
            this.skills = new ArrayList<>();
        }
        
        // Getters and setters
        public String getKeyword() {
            return keyword;
        }
        
        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public boolean isRemoteOnly() {
            return remoteOnly;
        }
        
        public void setRemoteOnly(boolean remoteOnly) {
            this.remoteOnly = remoteOnly;
        }
        
        public String getJobType() {
            return jobType;
        }
        
        public void setJobType(String jobType) {
            this.jobType = jobType;
        }
        
        public String getExperienceLevel() {
            return experienceLevel;
        }
        
        public void setExperienceLevel(String experienceLevel) {
            this.experienceLevel = experienceLevel;
        }
        
        public Double getMinSalary() {
            return minSalary;
        }
        
        public void setMinSalary(Double minSalary) {
            this.minSalary = minSalary;
        }
        
        public LocalDateTime getPostDateMin() {
            return postDateMin;
        }
        
        public void setPostDateMin(LocalDateTime postDateMin) {
            this.postDateMin = postDateMin;
        }
        
        public List<String> getSkills() {
            return skills;
        }
        
        public void setSkills(List<String> skills) {
            this.skills = skills;
        }
        
        // Checks if any filters are active
        public boolean hasActiveFilters() {
            return (keyword != null && !keyword.isEmpty()) ||
                   (location != null && !location.isEmpty()) ||
                   remoteOnly ||
                   (jobType != null && !jobType.isEmpty()) ||
                   (experienceLevel != null && !experienceLevel.isEmpty()) ||
                   minSalary != null ||
                   postDateMin != null ||
                   (skills != null && !skills.isEmpty());
        }
        
        // Returns a map of active filters for display
        public Map<String, String> getActiveFilters() {
            Map<String, String> filters = new LinkedHashMap<>();
            
            if (keyword != null && !keyword.isEmpty()) {
                filters.put("Keyword", keyword);
            }
            
            if (location != null && !location.isEmpty()) {
                filters.put("Location", location);
            }
            
            if (remoteOnly) {
                filters.put("Remote", "Yes");
            }
            
            if (jobType != null && !jobType.isEmpty()) {
                filters.put("Job Type", jobType);
            }
            
            if (experienceLevel != null && !experienceLevel.isEmpty()) {
                filters.put("Experience", experienceLevel);
            }
            
            if (minSalary != null) {
                filters.put("Min Salary", "$" + String.format("%,d", minSalary.intValue()));
            }
            
            if (postDateMin != null) {
                if (LocalDateTime.now().minusDays(1).compareTo(postDateMin) >= 0) {
                    filters.put("Date", "Past 24 hours");
                } else if (LocalDateTime.now().minusWeeks(1).compareTo(postDateMin) >= 0) {
                    filters.put("Date", "Past week");
                } else if (LocalDateTime.now().minusMonths(1).compareTo(postDateMin) >= 0) {
                    filters.put("Date", "Past month");
                }
            }
            
            return filters;
        }
    }

    public void bindClearProperties(
            SimpleBooleanProperty clearAllFilters,
            SimpleBooleanProperty clearKeyword,
            SimpleBooleanProperty clearLocation,
            SimpleBooleanProperty clearRemote,
            SimpleBooleanProperty clearJobType,
            SimpleBooleanProperty clearExperience,
            SimpleBooleanProperty clearSalary,
            SimpleBooleanProperty clearDate,
            SimpleBooleanProperty removeSkill) {
        
        // Listen for property changes and clear appropriate filters
        clearAllFilters.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                clearAllFilters();
                clearAllFilters.set(false);
            }
        });
        
        clearKeyword.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                searchField.clear();
                updateSearchCriteria();
                clearKeyword.set(false);
            }
        });
        
        clearLocation.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                locationField.clear();
                updateSearchCriteria();
                clearLocation.set(false);
            }
        });
        
        clearRemote.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                remoteCheckbox.setSelected(false);
                updateSearchCriteria();
                clearRemote.set(false);
            }
        });
        
        clearJobType.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                jobTypeComboBox.getSelectionModel().clearSelection();
                updateSearchCriteria();
                clearJobType.set(false);
            }
        });
        
        clearExperience.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                experienceLevelComboBox.getSelectionModel().clearSelection();
                updateSearchCriteria();
                clearExperience.set(false);
            }
        });
        
        clearSalary.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                salarySlider.setValue(0);
                updateSearchCriteria();
                clearSalary.set(false);
            }
        });
        
        clearDate.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                anyTimeRadio.setSelected(true);
                updateSearchCriteria();
                clearDate.set(false);
            }
        });
    }

    public void removeSkill(String skill) {
        if (skill != null && selectedSkills.contains(skill)) {
            selectedSkills.remove(skill);
            updateSkillsContainer();
            updateSearchCriteria();
        }
    }
}
