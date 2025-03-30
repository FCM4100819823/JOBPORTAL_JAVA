package com.jobportal.services;

import com.jobportal.models.User;
import com.jobportal.utils.DatabaseConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;

public class AuthService {

    private final MongoCollection<User> usersCollection;
    
    public AuthService() {
        this.usersCollection = DatabaseConnection.getDatabase().getCollection("users", User.class);
    }
    
    /**
     * Authenticate a user by username/email and password
     * @param usernameOrEmail The username or email
     * @param password The password
     * @return The authenticated user or null if authentication fails
     */
    public User authenticateUser(String usernameOrEmail, String password) {
        // Find user by username or email
        User user = usersCollection.find(
            Filters.or(
                Filters.eq("username", usernameOrEmail),
                Filters.eq("email", usernameOrEmail)
            )
        ).first();
        
        // If user exists and password matches
        if (user != null && verifyPassword(password, user.getPasswordHash())) {
            // Update last login time
            user.setLastLogin(LocalDateTime.now());
            usersCollection.replaceOne(
                Filters.eq("_id", user.getId()),
                user
            );
            return user;
        }
        
        return null;
    }
    
    /**
     * Register a new user
     * @param user The user to register
     * @param password The plain text password
     * @return The registered user
     */
    public User registerUser(User user, String password) {
        // Check if username or email already exists
        if (isUsernameTaken(user.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        
        if (isEmailTaken(user.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        // Hash the password
        user.setPasswordHash(hashPassword(password));
        
        // Set registration date
        user.setRegistrationDate(LocalDateTime.now());
        
        // Insert the user
        usersCollection.insertOne(user);
        
        return user;
    }
    
    /**
     * Register a new user (alias for registerUser for compatibility)
     * @param user The user to register
     * @param password The plain text password
     * @return The registered user
     */
    public User register(User user, String password) {
        return registerUser(user, password);
    }
    
    /**
     * Check if a username is already taken
     * @param username The username to check
     * @return True if taken, false otherwise
     */
    public boolean isUsernameTaken(String username) {
        return usersCollection.find(Filters.eq("username", username)).first() != null;
    }
    
    /**
     * Check if an email is already registered
     * @param email The email to check
     * @return True if taken, false otherwise
     */
    public boolean isEmailTaken(String email) {
        return usersCollection.find(Filters.eq("email", email)).first() != null;
    }
    
    /**
     * Check if an email exists in the database
     * @param email The email to check
     * @return True if the email exists, false otherwise
     */
    public boolean emailExists(String email) {
        return isEmailTaken(email);
    }
    
    /**
     * Find a user by their email
     * @param email The email to look for
     * @return The user if found, null otherwise
     */
    public User findUserByEmail(String email) {
        return usersCollection.find(Filters.eq("email", email)).first();
    }
    
    /**
     * Hash a password
     * @param password The plain text password
     * @return The hashed password
     */
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    
    /**
     * Verify a password against a hash
     * @param password The plain text password
     * @param hash The hashed password
     * @return True if the password matches, false otherwise
     */
    public boolean verifyPassword(String password, String hash) {
        return BCrypt.checkpw(password, hash);
    }
    
    /**
     * Reset a user's password
     * @param userId The user ID
     * @param newPassword The new password
     * @return True if successful, false otherwise
     */
    public boolean resetPassword(ObjectId userId, String newPassword) {
        User user = usersCollection.find(Filters.eq("_id", userId)).first();
        
        if (user != null) {
            user.setPasswordHash(hashPassword(newPassword));
            return usersCollection.replaceOne(
                Filters.eq("_id", userId),
                user
            ).getModifiedCount() > 0;
        }
        
        return false;
    }
}
