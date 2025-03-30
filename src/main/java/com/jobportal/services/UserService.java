package com.jobportal.services;

import com.jobportal.models.Job;
import com.jobportal.models.User;
import com.jobportal.utils.DatabaseConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

public class UserService {
    
    private final MongoCollection<User> usersCollection;
    private final JobService jobService;
    private final AuthService authService; // Add AuthService
    
    public UserService() {
        usersCollection = DatabaseConnection.getDatabase().getCollection("users", User.class);
        jobService = new JobService();
        authService = new AuthService(); // Initialize AuthService
    }
    
    /**
     * Get a user by ID
     * @param userId The user ID
     * @return The user, or null if not found
     */
    public User getUserById(ObjectId userId) {
        return usersCollection.find(Filters.eq("_id", userId)).first();
    }
    
    /**
     * Update a user's profile
     * @param user The user to update
     * @return True if successful, false otherwise
     */
    public boolean updateUser(User user) {
        return usersCollection.replaceOne(
                Filters.eq("_id", user.getId()),
                user
        ).getModifiedCount() > 0;
    }
    
    /**
     * Calculate the completion percentage of a user's profile
     * @param user The user
     * @return Profile completion percentage (0-100)
     */
    public int calculateProfileCompletion(User user) {
        int totalFields = 7; // username, email, fullName, phone, location, profilePicture, role
        int completedFields = 0;
        
        if (user.getUsername() != null && !user.getUsername().isEmpty()) completedFields++;
        if (user.getEmail() != null && !user.getEmail().isEmpty()) completedFields++;
        if (user.getFullName() != null && !user.getFullName().isEmpty()) completedFields++;
        if (user.getPhone() != null && !user.getPhone().isEmpty()) completedFields++;
        if (user.getLocation() != null && !user.getLocation().isEmpty()) completedFields++;
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) completedFields++;
        if (user.getRole() != null) completedFields++;
        
        return (int) Math.round((double) completedFields / totalFields * 100);
    }
    
    /**
     * Get the number of job applications for a user
     * @param userId The user ID
     * @return Number of applications
     */
    public int getApplicationCount(ObjectId userId) {
        // In a real application, you would query the applications collection
        return 0; // Placeholder
    }
    
    /**
     * Get the number of saved jobs for a user
     * @param userId The user ID
     * @return Number of saved jobs
     */
    public int getSavedJobsCount(ObjectId userId) {
        MongoCollection<Document> savedJobsCollection = 
                DatabaseConnection.getDatabase().getCollection("saved_jobs");
        
        return (int) savedJobsCollection.countDocuments(Filters.eq("userId", userId));
    }
    
    /**
     * Add a job to a user's saved jobs
     * @param userId The user ID
     * @param jobId The job ID
     * @return True if successful, false otherwise
     */
    public boolean saveJob(ObjectId userId, ObjectId jobId) {
        // First check if already saved to avoid duplicates
        if (isJobSaved(userId, jobId)) {
            return true;
        }
        
        MongoCollection<Document> savedJobsCollection = 
                DatabaseConnection.getDatabase().getCollection("saved_jobs");
        
        Document savedJob = new Document()
                .append("userId", userId)
                .append("jobId", jobId)
                .append("savedAt", new java.util.Date());
        
        savedJobsCollection.insertOne(savedJob);
        return true;
    }
    
    /**
     * Remove a job from a user's saved jobs
     * @param userId The user ID
     * @param jobId The job ID
     * @return True if successful, false otherwise
     */
    public boolean unsaveJob(ObjectId userId, ObjectId jobId) {
        MongoCollection<Document> savedJobsCollection = 
                DatabaseConnection.getDatabase().getCollection("saved_jobs");
        
        return savedJobsCollection.deleteOne(
                Filters.and(
                        Filters.eq("userId", userId),
                        Filters.eq("jobId", jobId)
                )
        ).getDeletedCount() > 0;
    }
    
    /**
     * Check if a job is saved by a user
     * @param userId The user ID
     * @param jobId The job ID
     * @return True if the job is saved by the user, false otherwise
     */
    public boolean isJobSaved(ObjectId userId, ObjectId jobId) {
        MongoCollection<Document> savedJobsCollection = 
                DatabaseConnection.getDatabase().getCollection("saved_jobs");
        
        return savedJobsCollection.find(
                Filters.and(
                        Filters.eq("userId", userId),
                        Filters.eq("jobId", jobId)
                )
        ).first() != null;
    }
    
    /**
     * Get all saved jobs for a user
     * @param userId The user ID
     * @return List of saved jobs with their metadata
     */
    public List<Document> getSavedJobsWithMetadata(ObjectId userId) {
        MongoCollection<Document> savedJobsCollection = 
                DatabaseConnection.getDatabase().getCollection("saved_jobs");
        
        List<Document> savedJobs = new ArrayList<>();
        savedJobsCollection.find(Filters.eq("userId", userId))
                .sort(Sorts.descending("savedAt"))
                .into(savedJobs);
        
        return savedJobs;
    }
    
    /**
     * Get all job objects that a user has saved
     * @param userId The user ID
     * @return List of Job objects
     */
    public List<Job> getSavedJobs(ObjectId userId) {
        List<Document> savedJobDocs = getSavedJobsWithMetadata(userId);
        List<Job> jobs = new ArrayList<>();
        
        for (Document doc : savedJobDocs) {
            ObjectId jobId = doc.getObjectId("jobId");
            Job job = jobService.getJobById(jobId);
            if (job != null) {
                jobs.add(job);
            }
        }
        
        return jobs;
    }
    
    /**
     * Get saved date for a specific job
     * @param userId The user ID
     * @param jobId The job ID
     * @return Saved date or null if not found
     */
    public Date getSavedDate(ObjectId userId, ObjectId jobId) {
        MongoCollection<Document> savedJobsCollection = 
                DatabaseConnection.getDatabase().getCollection("saved_jobs");
        
        Document savedJob = savedJobsCollection.find(
                Filters.and(
                        Filters.eq("userId", userId),
                        Filters.eq("jobId", jobId)
                )
        ).first();
        
        return savedJob != null ? savedJob.getDate("savedAt") : null;
    }
    
    /**
     * Categorize saved jobs for a user
     * @param userId The user ID
     * @return Map of category to job count
     */
    public java.util.Map<String, Integer> categorizeSavedJobs(ObjectId userId) {
        java.util.Map<String, Integer> categories = new java.util.HashMap<>();
        List<Job> savedJobs = getSavedJobs(userId);
        
        // Initialize default categories
        categories.put("All", savedJobs.size());
        categories.put("Recent", 0);
        
        // Group by employment type
        for (Job job : savedJobs) {
            // Count recent jobs (saved in last 7 days)
            Date savedDate = getSavedDate(userId, job.getId());
            if (savedDate != null) {
                long daysDiff = (new Date().getTime() - savedDate.getTime()) / (1000 * 60 * 60 * 24);
                if (daysDiff <= 7) {
                    categories.put("Recent", categories.get("Recent") + 1);
                }
            }
            
            // Count by employment type
            String type = job.getEmploymentType();
            if (type != null && !type.isEmpty()) {
                categories.put(type, categories.getOrDefault(type, 0) + 1);
            }
        }
        
        return categories;
    }

    /**
     * Get count of users that match the search query and role
     * @param searchQuery The search query (email, username, or name)
     * @param role The user role (nullable)
     * @return Count of matching users
     */
    public int getUserCount(String searchQuery, User.Role role) {
        Bson filter = createUserFilter(searchQuery, role);
        return (int) usersCollection.countDocuments(filter);
    }

    /**
     * Get users with pagination that match the search query and role
     * @param searchQuery The search query (email, username, or name)
     * @param role The user role (nullable)
     * @param page Page number (0-based)
     * @param pageSize Page size
     * @return List of users
     */
    public List<User> getPaginatedUsers(String searchQuery, User.Role role, int page, int pageSize) {
        List<User> users = new ArrayList<>();
        Bson filter = createUserFilter(searchQuery, role);
        
        usersCollection.find(filter)
                .sort(Sorts.descending("registrationDate"))
                .skip(page * pageSize)
                .limit(pageSize)
                .into(users);
        
        return users;
    }

    /**
     * Create a filter for user search
     * @param searchQuery The search query (email, username, or name)
     * @param role The user role (nullable)
     * @return Bson filter
     */
    private Bson createUserFilter(String searchQuery, User.Role role) {
        List<Bson> conditions = new ArrayList<>();
        
        // Add search query filter if provided
        if (searchQuery != null && !searchQuery.isEmpty()) {
            Pattern pattern = Pattern.compile(searchQuery, Pattern.CASE_INSENSITIVE);
            conditions.add(Filters.or(
                    Filters.regex("username", pattern),
                    Filters.regex("email", pattern),
                    Filters.regex("fullName", pattern)
            ));
        }
        
        // Add role filter if provided
        if (role != null) {
            conditions.add(Filters.eq("role", role));
        }
        
        // Create combined filter or empty filter if no conditions
        if (conditions.isEmpty()) {
            return new Document();
        } else {
            return Filters.and(conditions);
        }
    }

    /**
     * Delete a user by ID
     * @param userId The user ID
     * @return True if successful, false otherwise
     */
    public boolean deleteUser(ObjectId userId) {
        return usersCollection.deleteOne(Filters.eq("_id", userId)).getDeletedCount() > 0;
    }

    /**
     * Get recent users
     * @param limit Maximum number of users to return
     * @return List of recent users
     */
    public List<User> getRecentUsers(int limit) {
        List<User> users = new ArrayList<>();
        usersCollection.find()
                .sort(Sorts.descending("registrationDate"))
                .limit(limit)
                .into(users);
        return users;
    }

    /**
     * Get count of new users registered in the last hours
     * @param hours Number of hours
     * @return Count of new users
     */
    public int getNewUserCount(int hours) {
        LocalDateTime startDate = LocalDateTime.now().minusHours(hours);
        return (int) usersCollection.countDocuments(
                Filters.gte("registrationDate", startDate)
        );
    }

    // Add methods that were missing
    public boolean isUsernameTaken(String username) {
        return authService.isUsernameTaken(username);
    }
    
    public String hashPassword(String password) {
        return authService.hashPassword(password);
    }

    /**
     * Get total user count
     * @return Number of users
     */
    public int getUserCount() {
        return (int) usersCollection.countDocuments();
    }
    
    /**
     * Create a new user
     * @param user The user to create
     * @return The created user
     */
    public User createUser(User user) {
        usersCollection.insertOne(user);
        return user;
    }
    
    /**
     * Check if an email is already taken (delegate to AuthService)
     * @param email The email to check
     * @return True if the email is taken, false otherwise
     */
    public boolean isEmailTaken(String email) {
        return authService.isEmailTaken(email);
    }

}
