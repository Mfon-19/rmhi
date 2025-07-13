package com.mfon.rmhi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {


    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options = null;
        try {
            GoogleCredentials creds = GoogleCredentials.fromStream(
                    new FileInputStream("/Users/mfonudoh/Desktop/Programs/eureka/backend/src/main/resources/secrets/firebase/serviceAccount.json"));

            options = FirebaseOptions.builder()
                    .setCredentials(creds)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
            throw new  IOException();
        }
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }
}


