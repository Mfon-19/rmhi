package com.mfon.rmhi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {
    @Bean
    FirebaseApp firebaseApp() throws IOException {
        GoogleCredentials creds = GoogleCredentials.fromStream(
                new FileInputStream("/Users/mfonudoh/Desktop/Programs/eureka/backend/src/main/resources/secrets/firebase/serviceAccount.json"));

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(creds)
                .build();

        return FirebaseApp.initializeApp(options);
    }
}

