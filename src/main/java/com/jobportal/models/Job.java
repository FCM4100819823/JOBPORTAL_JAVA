package com.jobportal.models;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Job {
    
    @BsonId
    private ObjectId id;
    private ObjectId employerId;
    private String title;
    private String company;
    private String location;
    private String description;
    private String employmentType;  // Full-time, Part-time, Contract, etc.
    private String experienceLevel; // Entry Level, Junior, Mid-Level, Senior, etc.
    private Double salaryMin;
    private Double salaryMax;
    private String currency;        // $, €, £, etc.
    private Integer numberOfPositions;
    private List<String> skills;
    private LocalDateTime postDate;
    private LocalDateTime applicationDeadline;
    private String applicationInstructions;
    private boolean isActive;
    private int viewCount;
    private int applicationCount;
    
    // Constructor
    public Job() {
        this.skills = new ArrayList<>();
        this.isActive = true;
        this.viewCount = 0;
        this.applicationCount = 0;
    }
    
    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getEmployerId() {
        return employerId;
    }

    public void setEmployerId(ObjectId employerId) {
        this.employerId = employerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public Double getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(Double salaryMin) {
        this.salaryMin = salaryMin;
    }

    public Double getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(Double salaryMax) {
        this.salaryMax = salaryMax;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getNumberOfPositions() {
        return numberOfPositions;
    }

    public void setNumberOfPositions(Integer numberOfPositions) {
        this.numberOfPositions = numberOfPositions;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public LocalDateTime getPostDate() {
        return postDate;
    }

    public void setPostDate(LocalDateTime postDate) {
        this.postDate = postDate;
    }

    public LocalDateTime getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(LocalDateTime applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public String getApplicationInstructions() {
        return applicationInstructions;
    }

    public void setApplicationInstructions(String applicationInstructions) {
        this.applicationInstructions = applicationInstructions;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getApplicationCount() {
        return applicationCount;
    }

    public void setApplicationCount(int applicationCount) {
        this.applicationCount = applicationCount;
    }

    // Helper methods for status display
    public String getStatusDisplayText() {
        return isActive ? "Active" : "Inactive";
    }
    
    // Helper method to check if job is expired
    public boolean isExpired() {
        if (applicationDeadline == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(applicationDeadline);
    }
    
    public String getEmploymentTypeDisplayText() {
        return employmentType != null ? employmentType : "Not specified";
    }
    
    public String getExperienceLevelDisplayText() {
        return experienceLevel != null ? experienceLevel : "Not specified";
    }
    
    public String getSalaryDisplayText() {
        if (salaryMin == null && salaryMax == null) {
            return "Not specified";
        }
        
        String currencySymbol = currency != null ? currency : "$";
        
        if (salaryMin != null && salaryMax != null) {
            return currencySymbol + String.format("%,d", salaryMin.intValue()) + 
                   " - " + currencySymbol + String.format("%,d", salaryMax.intValue());
        } else if (salaryMin != null) {
            return currencySymbol + String.format("%,d", salaryMin.intValue()) + "+";
        } else {
            return "Up to " + currencySymbol + String.format("%,d", salaryMax.intValue());
        }
    }
}
