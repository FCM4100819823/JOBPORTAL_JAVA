package com.jobportal.services;

import com.jobportal.models.Education;
import com.jobportal.models.Experience;
import com.jobportal.utils.DatabaseConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class ProfileService {
    
    private final MongoCollection<Education> educationCollection;
    private final MongoCollection<Experience> experienceCollection;
    
    public ProfileService() {
        educationCollection = DatabaseConnection.getDatabase().getCollection("education", Education.class);
        experienceCollection = DatabaseConnection.getDatabase().getCollection("experience", Experience.class);
    }
    
    /**
     * Get all education entries for a user
     * @param userId The user ID
     * @return List of education entries
     */
    public List<Education> getUserEducation(ObjectId userId) {
        List<Education> educationList = new ArrayList<>();
        educationCollection.find(Filters.eq("userId", userId))
                .sort(com.mongodb.client.model.Sorts.descending("endDate"))
                .into(educationList);
        return educationList;
    }
    
    /**
     * Add a new education entry
     * @param education The education entry to add
     * @return The added education entry
     */
    public Education addEducation(Education education) {
        educationCollection.insertOne(education);
        return education;
    }
    
    /**
     * Update an existing education entry
     * @param education The education entry to update
     * @return True if successful, false otherwise
     */
    public boolean updateEducation(Education education) {
        return educationCollection.replaceOne(
                Filters.eq("_id", education.getId()),
                education
        ).getModifiedCount() > 0;
    }
    
    /**
     * Delete an education entry
     * @param educationId The education entry ID
     * @return True if successful, false otherwise
     */
    public boolean deleteEducation(ObjectId educationId) {
        return educationCollection.deleteOne(
                Filters.eq("_id", educationId)
        ).getDeletedCount() > 0;
    }
    
    /**
     * Get all experience entries for a user
     * @param userId The user ID
     * @return List of experience entries
     */
    public List<Experience> getUserExperience(ObjectId userId) {
        List<Experience> experienceList = new ArrayList<>();
        experienceCollection.find(Filters.eq("userId", userId))
                .sort(com.mongodb.client.model.Sorts.descending("endDate"))
                .into(experienceList);
        return experienceList;
    }
    
    /**
     * Add a new experience entry
     * @param experience The experience entry to add
     * @return The added experience entry
     */
    public Experience addExperience(Experience experience) {
        experienceCollection.insertOne(experience);
        return experience;
    }
    
    /**
     * Update an existing experience entry
     * @param experience The experience entry to update
     * @return True if successful, false otherwise
     */
    public boolean updateExperience(Experience experience) {
        return experienceCollection.replaceOne(
                Filters.eq("_id", experience.getId()),
                experience
        ).getModifiedCount() > 0;
    }
    
    /**
     * Delete an experience entry
     * @param experienceId The experience entry ID
     * @return True if successful, false otherwise
     */
    public boolean deleteExperience(ObjectId experienceId) {
        return experienceCollection.deleteOne(
                Filters.eq("_id", experienceId)
        ).getDeletedCount() > 0;
    }
}
