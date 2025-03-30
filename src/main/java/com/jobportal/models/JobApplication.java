package com.jobportal.models;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public class JobApplication {
    
    public enum Status {
        SUBMITTED,
        UNDER_REVIEW,
        INTERVIEW_SCHEDULED,
        OFFER_EXTENDED,
        OFFER_ACCEPTED,
        OFFER_DECLINED,
        REJECTED,
        WITHDRAWN
    }
    
    @BsonId
    private ObjectId id;
    private ObjectId jobId;
    private ObjectId applicantId;
    private String fullName;
    private String email;
    private String phone;
    private String location;
    private String resumePath;
    private String coverLetter;
    private Status status;
    private LocalDateTime submissionDate;
    private LocalDateTime lastUpdateDate;
    private String employerNotes;
    
    // Constructor
    public JobApplication() {
        this.status = Status.SUBMITTED;
        this.submissionDate = LocalDateTime.now();
        this.lastUpdateDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getJobId() {
        return jobId;
    }

    public void setJobId(ObjectId jobId) {
        this.jobId = jobId;
    }

    public ObjectId getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(ObjectId applicantId) {
        this.applicantId = applicantId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public LocalDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(LocalDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getEmployerNotes() {
        return employerNotes;
    }

    public void setEmployerNotes(String employerNotes) {
        this.employerNotes = employerNotes;
    }
    
    // Helper methods
    public String getStatusDisplay() {
        if (status == null) return "Unknown";
        
        return switch (status) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under Review";
            case INTERVIEW_SCHEDULED -> "Interview Scheduled";
            case OFFER_EXTENDED -> "Offer Extended";
            case OFFER_ACCEPTED -> "Offer Accepted";
            case OFFER_DECLINED -> "Offer Declined";
            case REJECTED -> "Rejected";
            case WITHDRAWN -> "Withdrawn";
        };
    }
}
