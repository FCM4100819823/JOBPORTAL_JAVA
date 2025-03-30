package com.jobportal.services;

import com.jobportal.models.JobApplication;
import com.jobportal.models.Job;
import com.jobportal.utils.DatabaseConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationService {
    
    private final MongoCollection<JobApplication> applicationsCollection;
    
    public ApplicationService() {
        this.applicationsCollection = DatabaseConnection.getDatabase().getCollection("applications", JobApplication.class);
    }
    
    /**
     * Submit a job application
     * @param application The application to submit
     * @return The submitted application
     */
    public JobApplication submitApplication(JobApplication application) {
        // Check if user has already applied
        if (hasApplied(application.getJobId(), application.getApplicantId())) {
            throw new IllegalStateException("You have already applied for this job");
        }
        
        // Set submission date and initial status
        application.setSubmissionDate(java.time.LocalDateTime.now());
        application.setStatus(JobApplication.Status.SUBMITTED);
        
        // Insert into database
        applicationsCollection.insertOne(application);
        
        return application;
    }
    
    /**
     * Check if a user has already applied for a job
     * @param jobId The job ID
     * @param userId The user ID
     * @return True if the user has already applied, false otherwise
     */
    public boolean hasApplied(ObjectId jobId, ObjectId userId) {
        return applicationsCollection.find(
                Filters.and(
                        Filters.eq("jobId", jobId),
                        Filters.eq("applicantId", userId)
                )
        ).first() != null;
    }
    
    /**
     * Get applications for a job
     * @param jobId The job ID
     * @return List of applications
     */
    public List<JobApplication> getApplicationsForJob(ObjectId jobId) {
        List<JobApplication> applications = new ArrayList<>();
        applicationsCollection.find(Filters.eq("jobId", jobId))
                .sort(Sorts.descending("submissionDate"))
                .into(applications);
        return applications;
    }
    
    /**
     * Get applications by a user
     * @param userId The user ID
     * @return List of applications
     */
    public List<JobApplication> getApplicationsByUser(ObjectId userId) {
        List<JobApplication> applications = new ArrayList<>();
        applicationsCollection.find(Filters.eq("applicantId", userId))
                .sort(Sorts.descending("submissionDate"))
                .into(applications);
        return applications;
    }
    
    /**
     * Get application by ID
     * @param applicationId The application ID
     * @return The application, or null if not found
     */
    public JobApplication getApplicationById(ObjectId applicationId) {
        return applicationsCollection.find(Filters.eq("_id", applicationId)).first();
    }
    
    /**
     * Update application status
     * @param applicationId The application ID
     * @param status The new status
     * @return True if successful, false otherwise
     */
    public boolean updateApplicationStatus(ObjectId applicationId, JobApplication.Status status) {
        return applicationsCollection.updateOne(
                Filters.eq("_id", applicationId),
                new org.bson.Document("$set", new org.bson.Document("status", status))
        ).getModifiedCount() > 0;
    }
    
    /**
     * Update employer notes
     * @param applicationId The application ID
     * @param notes The new notes
     * @return True if successful, false otherwise
     */
    public boolean updateEmployerNotes(ObjectId applicationId, String notes) {
        return applicationsCollection.updateOne(
                Filters.eq("_id", applicationId),
                new org.bson.Document("$set", new org.bson.Document("employerNotes", notes))
        ).getModifiedCount() > 0;
    }
    
    /**
     * Get application count for a job
     * @param jobId The job ID
     * @return Number of applications
     */
    public int getApplicationCount(ObjectId jobId) {
        return (int) applicationsCollection.countDocuments(Filters.eq("jobId", jobId));
    }
    
    /**
     * Get applications statistics by employer
     * @param employerId The employer ID
     * @return Map of status to count
     */
    public Map<JobApplication.Status, Integer> getApplicationStatsByEmployer(ObjectId employerId) {
        Map<JobApplication.Status, Integer> stats = new HashMap<>();
        
        // Initialize all statuses with 0
        for (JobApplication.Status status : JobApplication.Status.values()) {
            stats.put(status, 0);
        }
        
        // Get job IDs for this employer
        List<ObjectId> jobIds = new ArrayList<>();
        DatabaseConnection.getDatabase().getCollection("jobs")
                .find(Filters.eq("employerId", employerId))
                .projection(new org.bson.Document("_id", 1))
                .into(new ArrayList<>())
                .forEach(doc -> jobIds.add(doc.getObjectId("_id")));
        
        if (jobIds.isEmpty()) {
            return stats;
        }
        
        // Count applications by status
        List<JobApplication> applications = new ArrayList<>();
        applicationsCollection.find(Filters.in("jobId", jobIds))
                .into(applications);
        
        for (JobApplication app : applications) {
            JobApplication.Status status = app.getStatus();
            stats.put(status, stats.get(status) + 1);
        }
        
        return stats;
    }
    
    /**
     * Get total application count
     * @return Total number of applications
     */
    public int getTotalApplicationCount() {
        return (int) applicationsCollection.countDocuments();
    }
    
    /**
     * Get all applications submitted by a specific user
     * @param applicantId The applicant's ID
     * @return List of applications
     */
    public List<JobApplication> getApplicationsByApplicant(ObjectId applicantId) {
        List<JobApplication> applications = new ArrayList<>();
        applicationsCollection.find(Filters.eq("applicantId", applicantId))
                .sort(com.mongodb.client.model.Sorts.descending("submissionDate"))
                .into(applications);
        return applications;
    }
    
    /**
     * Withdraw an application
     * @param applicationId The application ID
     * @return True if successful, false otherwise
     */
    public boolean withdrawApplication(ObjectId applicationId) {
        return updateApplicationStatus(applicationId, JobApplication.Status.WITHDRAWN);
    }
    
    /**
     * Get all applications by a specific status for a user
     * @param applicantId The applicant's ID
     * @param status The application status
     * @return List of applications
     */
    public List<JobApplication> getApplicationsByStatus(ObjectId applicantId, JobApplication.Status status) {
        List<JobApplication> applications = new ArrayList<>();
        applicationsCollection.find(
                Filters.and(
                        Filters.eq("applicantId", applicantId),
                        Filters.eq("status", status)
                )
        ).sort(Sorts.descending("submissionDate"))
        .into(applications);
        return applications;
    }
    
    /**
     * Get application statistics for a user
     * @param applicantId The applicant's ID
     * @return Map with status counts
     */
    public java.util.Map<JobApplication.Status, Integer> getApplicationStats(ObjectId applicantId) {
        java.util.Map<JobApplication.Status, Integer> stats = new java.util.HashMap<>();
        
        // Initialize all statuses with 0
        for (JobApplication.Status status : JobApplication.Status.values()) {
            stats.put(status, 0);
        }
        
        // Get actual counts
        List<JobApplication> applications = getApplicationsByApplicant(applicantId);
        for (JobApplication application : applications) {
            JobApplication.Status status = application.getStatus();
            stats.put(status, stats.get(status) + 1);
        }
        
        return stats;
    }
    
    /**
     * Get applications that need attention (new or under review) for an employer
     * @param employerId The employer's ID
     * @param limit Maximum number of applications to return
     * @return List of applications
     */
    public List<JobApplication> getRecentApplicationsForEmployer(ObjectId employerId, int limit) {
        // Get all jobs by this employer
        JobService jobService = new JobService();
        List<Job> jobs = jobService.getJobsByEmployer(employerId);
        List<ObjectId> jobIds = jobs.stream().map(Job::getId).toList();
        
        // Get applications for these jobs that are new or under review
        List<JobApplication> applications = new ArrayList<>();
        applicationsCollection.find(
                Filters.and(
                        Filters.in("jobId", jobIds),
                        Filters.or(
                                Filters.eq("status", JobApplication.Status.SUBMITTED),
                                Filters.eq("status", JobApplication.Status.UNDER_REVIEW)
                        )
                )
        ).sort(com.mongodb.client.model.Sorts.descending("submissionDate"))
        .limit(limit)
        .into(applications);
        
        return applications;
    }
}
