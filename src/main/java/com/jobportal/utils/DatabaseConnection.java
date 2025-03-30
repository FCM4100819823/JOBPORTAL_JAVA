package com.jobportal.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import com.mongodb.MongoClientSettings;

public class DatabaseConnection {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "jobportal";
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    static {
        // Configure codec registry for POJOs
        CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        // Create client with POJO codec
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString(CONNECTION_STRING))
                .codecRegistry(pojoCodecRegistry)
                .build();

        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase(DATABASE_NAME);
        
        // Ensure collections exist
        ensureCollectionsExist();
    }

    private static void ensureCollectionsExist() {
        // List of all required collections
        String[] collections = {
            "jobs",           // Job postings
            "users",          // Users (job seekers and employers)
            "applications",   // Job applications
            "companies",      // Company profiles
            "resumes",        // User resumes/CVs
            "messages",       // Messages between users
            "notifications"   // User notifications
        };

        // Get existing collections
        for (String collectionName : collections) {
            try {
                // Check if collection exists, if not create it
                if (!collectionExists(collectionName)) {
                    database.createCollection(collectionName);
                    System.out.println("Created collection: " + collectionName);
                }
            } catch (Exception e) {
                System.err.println("Error creating collection " + collectionName + ": " + e.getMessage());
            }
        }
    }

    private static boolean collectionExists(String collectionName) {
        for (String name : database.listCollectionNames()) {
            if (name.equals(collectionName)) {
                return true;
            }
        }
        return false;
    }

    public static MongoDatabase getDatabase() {
        return database;
    }
    
    // Rename the method to match what's being called in App.java
    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
