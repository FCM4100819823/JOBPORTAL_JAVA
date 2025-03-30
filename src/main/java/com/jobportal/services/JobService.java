package com.jobportal.services;

import com.jobportal.models.Job;
import com.jobportal.models.User;
import com.jobportal.utils.DatabaseConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class JobService {
    private final MongoCollection<Job> jobsCollection;

    public JobService() {
        this.jobsCollection = DatabaseConnection.getDatabase().getCollection("jobs", Job.class);
    }

    /**
     * Get a list of all active jobs
     * @return List of jobs
     */
    public List<Job> getAllActiveJobs() {
        List<Job> jobs = new ArrayList<>();
        jobsCollection.find(Filters.eq("isActive", true))
                .sort(Sorts.descending("postDate"))
                .into(jobs);
        return jobs;
    }

    /**
     * Get jobs posted by a specific employer
     * @param employerId The employer's ID
     * @return List of jobs
     */
    public List<Job> getJobsByEmployer(ObjectId employerId) {
        List<Job> jobs = new ArrayList<>();
        jobsCollection.find(Filters.eq("employerId", employerId))
                .sort(Sorts.descending("postDate"))
                .into(jobs);
        return jobs;
    }

    /**
     * Get active jobs posted by a specific employer
     * @param employerId The employer's ID
     * @return List of active jobs
     */
    public List<Job> getActiveJobsByEmployer(ObjectId employerId) {
        List<Job> jobs = new ArrayList<>();
        jobsCollection.find(Filters.and(
                Filters.eq("employerId", employerId),
                Filters.eq("isActive", true)
        )).sort(Sorts.descending("postDate"))
          .into(jobs);
        return jobs;
    }

    /**
     * Get inactive jobs posted by a specific employer
     * @param employerId The employer's ID
     * @return List of inactive jobs
     */
    public List<Job> getInactiveJobsByEmployer(ObjectId employerId) {
        List<Job> jobs = new ArrayList<>();
        jobsCollection.find(Filters.and(
                Filters.eq("employerId", employerId),
                Filters.eq("isActive", false)
        )).sort(Sorts.descending("postDate"))
          .into(jobs);
        return jobs;
    }

    /**
     * Get a specific job by ID
     * @param jobId The job ID
     * @return The job, or null if not found
     */
    public Job getJobById(ObjectId jobId) {
        return jobsCollection.find(Filters.eq("_id", jobId)).first();
    }

    /**
     * Create a new job posting
     * @param job The job to create
     * @return The created job
     */
    public Job createJob(Job job) {
        job.setPostDate(LocalDateTime.now());
        job.setActive(true);
        jobsCollection.insertOne(job);
        return job;
    }

    /**
     * Update an existing job
     * @param job The job to update
     * @return True if successful, false otherwise
     */
    public boolean updateJob(Job job) {
        return jobsCollection.replaceOne(
                Filters.eq("_id", job.getId()),
                job
        ).getModifiedCount() > 0;
    }

    /**
     * Delete a job
     * @param jobId The job ID
     * @return True if successful, false otherwise
     */
    public boolean deleteJob(ObjectId jobId) {
        return jobsCollection.deleteOne(Filters.eq("_id", jobId)).getDeletedCount() > 0;
    }

    /**
     * Search for jobs based on keywords
     * @param searchTerm The search term
     * @return List of matching jobs
     */
    public List<Job> searchJobs(String searchTerm) {
        List<Job> jobs = new ArrayList<>();
        jobsCollection.find(Filters.and(
                Filters.eq("isActive", true),
                Filters.or(
                        Filters.regex("title", searchTerm, "i"),
                        Filters.regex("description", searchTerm, "i"),
                        Filters.regex("company", searchTerm, "i"),
                        Filters.regex("location", searchTerm, "i")
                )
        )).sort(Sorts.descending("postDate"))
          .into(jobs);
        return jobs;
    }

    /**
     * Search for jobs with advanced filtering options
     * @param searchTerm Search keywords (title, description, company)
     * @param location Job location
     * @param employmentType Type of employment (full-time, part-time, etc.)
     * @param experienceLevel Experience level required
     * @param minSalary Minimum salary
     * @return List of matching jobs
     */
    public List<Job> searchJobsAdvanced(String searchTerm, String location, 
                                        String employmentType, String experienceLevel,
                                        Double minSalary) {
        List<Bson> filters = new ArrayList<>();

        // Always filter for active jobs
        filters.add(Filters.eq("isActive", true));
        
        // Add search term filter if provided
        if (searchTerm != null && !searchTerm.isEmpty()) {
            filters.add(Filters.or(
                Filters.regex("title", searchTerm, "i"),
                Filters.regex("description", searchTerm, "i"),
                Filters.regex("company", searchTerm, "i")
            ));
        }
        
        // Add location filter if provided
        if (location != null && !location.isEmpty()) {
            filters.add(Filters.regex("location", location, "i"));
        }

        // Add employment type filter if provided
        if (employmentType != null && !employmentType.isEmpty()) {
            filters.add(Filters.eq("employmentType", employmentType));
        }

        // Add experience level filter if provided
        if (experienceLevel != null && !experienceLevel.isEmpty()) {
            filters.add(Filters.eq("experienceLevel", experienceLevel));
        }

        // Add minimum salary filter if provided
        if (minSalary != null) {
            filters.add(Filters.gte("salaryMin", minSalary));
        }

        // Execute the query
        List<Job> jobs = new ArrayList<>();
        jobsCollection.find(Filters.and(filters))
                .sort(Sorts.descending("postDate"))
                .into(jobs);
        return jobs;
    }

    /**
     * Get suggested locations for autocomplete
     * @param prefix The location prefix to match
     * @return List of unique locations that start with the prefix
     */
    public List<String> getSuggestedLocations(String prefix) {
        List<String> locations = new ArrayList<>();
        jobsCollection.distinct("location", String.class)
                .filter(Filters.regex("location", "^" + prefix, "i"))
                .into(locations);
        return locations;
    }

    /**
     * Get all available employment types
     * @return List of unique employment types
     */
    public List<String> getAllEmploymentTypes() {
        return List.of("Full-time", "Part-time", "Contract", "Temporary", "Internship", "Freelance");
    }

    /**
     * Get all available experience levels
     * @return List of unique experience levels
     */
    public List<String> getAllExperienceLevels() {
        return List.of("Entry Level", "Junior", "Mid-Level", "Senior", "Lead", "Manager", "Director", "Executive");
    }

    /**
     * Get recommended jobs for a user based on their profile
     * @param user The user to get recommendations for
     * @return List of recommended jobs
     */
    public List<Job> getRecommendedJobs(User user) {
        // In a real app, this would use a more sophisticated algorithm
        // For now, just return the most recent jobs
        return getAllActiveJobs();
    }

    /**
     * Get similar jobs to a given job
     * @param job The job to find similar jobs for
     * @param limit Maximum number of jobs to return
     * @return List of similar jobs
     */
    public List<Job> getSimilarJobs(Job job, int limit) {
        // Create filters for similarity
        List<Bson> filters = new ArrayList<>();

        // Always filter for active jobs
        filters.add(Filters.eq("isActive", true));
        
        // Filter for similar title or similar skills
        List<Bson> similarityFilters = new ArrayList<>();
        
        // Similar title (contains part of the original title)
        String[] titleWords = job.getTitle().split("\\s+");
        for (String word : titleWords) {
            if (word.length() > 3) { // Only use meaningful words
                similarityFilters.add(Filters.regex("title", word, "i"));
            }
        }
        
        // Similar skills
        if (job.getSkills() != null && !job.getSkills().isEmpty()) {
            for (String skill : job.getSkills()) {
                similarityFilters.add(Filters.all("skills", skill));
            }
        }
        
        // Similar location
        similarityFilters.add(Filters.eq("location", job.getLocation()));
        
        // Similar experience level
        if (job.getExperienceLevel() != null) {
            similarityFilters.add(Filters.eq("experienceLevel", job.getExperienceLevel()));
        }

        // Combine similarity filters with OR
        filters.add(Filters.or(similarityFilters));

        // Execute the query
        List<Job> similarJobs = new ArrayList<>();
        jobsCollection.find(Filters.and(filters))
                .limit(limit + 1) // Get one extra to account for the current job
                .into(similarJobs);
        return similarJobs;
    }

    /**
     * Get total count of jobs
     * @return Total number of jobs
     */
    public int getTotalJobCount() {
        return (int) jobsCollection.countDocuments();
    }
    
    /**
     * Get count of jobs matching search criteria
     * @param searchQuery The search query
     * @param isActive Filter for active or inactive jobs (null for both)
     * @return Count of matching jobs
     */
    public int getJobsCount(String searchQuery, Boolean isActive) {
        List<Bson> filters = new ArrayList<>();
        
        // Add search term filter if provided
        if (searchQuery != null && !searchQuery.isEmpty()) {
            filters.add(Filters.or(
                Filters.regex("title", Pattern.compile(searchQuery, Pattern.CASE_INSENSITIVE)),
                Filters.regex("company", Pattern.compile(searchQuery, Pattern.CASE_INSENSITIVE)),
                Filters.regex("location", Pattern.compile(searchQuery, Pattern.CASE_INSENSITIVE))
            ));
        }
        
        // Add active filter if provided
        if (isActive != null) {
            filters.add(Filters.eq("isActive", isActive));
        }
        
        // Execute the query with filters
        if (filters.isEmpty()) {
            return (int) jobsCollection.countDocuments();
        } else {
            return (int) jobsCollection.countDocuments(Filters.and(filters));
        }
    }
    
    /**
     * Get paginated jobs matching search criteria
     * @param searchQuery The search query
     * @param isActive Filter for active or inactive jobs (null for both)
     * @param page Page number (0-based)
     * @param pageSize Number of jobs per page
     * @return List of matching jobs
     */
    public List<Job> getPaginatedJobs(String searchQuery, Boolean isActive, int page, int pageSize) {
        List<Bson> filters = new ArrayList<>();
        
        // Add search term filter if provided
        if (searchQuery != null && !searchQuery.isEmpty()) {
            filters.add(Filters.or(
                Filters.regex("title", Pattern.compile(searchQuery, Pattern.CASE_INSENSITIVE)),
                Filters.regex("company", Pattern.compile(searchQuery, Pattern.CASE_INSENSITIVE)),
                Filters.regex("location", Pattern.compile(searchQuery, Pattern.CASE_INSENSITIVE))
            ));
        }
        
        // Add active filter if provided
        if (isActive != null) {
            filters.add(Filters.eq("isActive", isActive));
        }
        
        // Execute the query with pagination
        List<Job> jobs = new ArrayList<>();
        if (filters.isEmpty()) {
            jobsCollection.find()
                    .sort(Sorts.descending("postDate"))
                    .skip(page * pageSize)
                    .limit(pageSize)
                    .into(jobs);
        } else {
            jobsCollection.find(Filters.and(filters))
                    .sort(Sorts.descending("postDate"))
                    .skip(page * pageSize)
                    .limit(pageSize)
                    .into(jobs);
        }
        
        return jobs;
    }
    
    /**
     * Get most recent jobs
     * @param limit Maximum number of jobs to return
     * @return List of recent jobs
     */
    public List<Job> getRecentJobs(int limit) {
        List<Job> jobs = new ArrayList<>();
        jobsCollection.find()
                .sort(Sorts.descending("postDate"))
                .limit(limit)
                .into(jobs);
        return jobs;
    }
    
    /**
     * Search jobs with advanced criteria
     * @param criteria The search criteria object
     * @param sortField Field to sort by
     * @param page Page number (0-based)
     * @param pageSize Number of jobs per page
     * @return List of matching jobs
     */
    public List<Job> searchJobs(Object criteria, String sortField, int page, int pageSize) {
        // This is a stub implementation to handle the method signature mismatch
        // Ideally this would properly implement the search using the criteria object
        List<Job> jobs = new ArrayList<>();
        jobsCollection.find(Filters.eq("isActive", true))
                .sort(Sorts.descending("postDate"))
                .skip(page * pageSize)
                .limit(pageSize)
                .into(jobs);
        return jobs;
    }
    
    /**
     * Get count of jobs matching criteria
     * @param criteria The search criteria
     * @return Count of matching jobs
     */
    public int getJobsCount(Object criteria) {
        // This is a stub implementation to handle the method signature mismatch
        return (int) jobsCollection.countDocuments(Filters.eq("isActive", true));
    }
}
