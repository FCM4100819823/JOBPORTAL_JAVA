package com.jobportal.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class EmailService {
    
    // For demo purposes, we'll store verification codes in memory
    // In a production app, this should use a real email service
    private final Map<String, String> verificationCodes = new HashMap<>();
    
    /**
     * Send a password reset verification code to the user's email
     * @param email The email address to send the code to
     * @return The verification code that was sent
     */
    public String sendPasswordResetCode(String email) {
        // Generate a random 6-digit code
        String verificationCode = generateVerificationCode();
        
        // Store the code for verification later
        verificationCodes.put(email, verificationCode);
        
        // In a real application, you would send an actual email here
        System.out.println("Sending verification code to " + email + ": " + verificationCode);
        
        return verificationCode;
    }
    
    /**
     * Verify a code for a specific email
     * @param email The email the code was sent to
     * @param code The code to verify
     * @return True if the code is valid, false otherwise
     */
    public boolean verifyCode(String email, String code) {
        String storedCode = verificationCodes.get(email);
        return storedCode != null && storedCode.equals(code);
    }
    
    /**
     * Remove a verification code after it has been used
     * @param email The email to remove the code for
     */
    public void clearVerificationCode(String email) {
        verificationCodes.remove(email);
    }
    
    /**
     * Generate a random 6-digit verification code
     * @return A 6-digit code as a string
     */
    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }
    
    /**
     * Send a verification code to an email
     * @param email The email address
     * @param code The verification code
     * @return True if successful, false otherwise
     */
    public boolean sendVerificationCode(String email, String code) {
        // In a real application, this would connect to an email service
        // For now, we'll just simulate success
        System.out.println("Sending verification code " + code + " to " + email);
        return true;
    }
    
    /**
     * Send a password reset link to an email
     * @param email The email address
     * @param resetLink The reset link
     * @return True if successful, false otherwise
     */
    public boolean sendPasswordResetLink(String email, String resetLink) {
        // In a real application, this would connect to an email service
        System.out.println("Sending password reset link " + resetLink + " to " + email);
        return true;
    }
    
    /**
     * Send an application confirmation email
     * @param email The applicant's email
     * @param jobTitle The job title
     * @param company The company name
     * @return True if successful, false otherwise
     */
    public boolean sendApplicationConfirmation(String email, String jobTitle, String company) {
        // In a real application, this would connect to an email service
        System.out.println("Sending application confirmation for " + jobTitle + " at " + company + " to " + email);
        return true;
    }
}
