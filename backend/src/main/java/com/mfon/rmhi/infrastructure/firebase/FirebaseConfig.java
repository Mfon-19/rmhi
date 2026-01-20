package com.mfon.rmhi.infrastructure.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private final FirebaseProperties properties;

    public FirebaseConfig(FirebaseProperties properties) {
        this.properties = properties;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials creds = loadCredentials();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(creds)
                .build();
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        String json = properties.getServiceAccountJson();
        if (json != null && !json.isBlank()) {
            try (InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(input);
            }
        }

        String path = properties.getServiceAccountPath();
        if (path != null && !path.isBlank()) {
            try (InputStream input = new FileInputStream(path)) {
                return GoogleCredentials.fromStream(input);
            }
        }

        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (envPath != null && !envPath.isBlank()) {
            try (InputStream input = new FileInputStream(envPath)) {
                return GoogleCredentials.fromStream(input);
            }
        }

        throw new IllegalStateException("Firebase credentials not configured.");
    }
}

