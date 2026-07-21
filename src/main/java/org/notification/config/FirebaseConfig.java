package org.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }
            
            FirebaseOptions options = null;
            InputStream serviceAccount = null;
            
            // 1. Try environment variable path
            String credentialPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
            if (credentialPath != null && Files.exists(Paths.get(credentialPath))) {
                serviceAccount = Files.newInputStream(Paths.get(credentialPath));
                log.info("Loading Firebase credentials from path: {}", credentialPath);
            }
            
            // 2. Try classpath resource
            if (serviceAccount == null) {
                serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                if (serviceAccount != null) {
                    log.info("Loading Firebase credentials from classpath (firebase-service-account.json)");
                }
            }
            
            if (serviceAccount != null) {
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
            } else {
                // 3. Fallback to application default credentials (if running on GCP/AWS with IAM roles)
                try {
                    log.info("No explicit Firebase credentials found. Trying Google Application Default Credentials...");
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.getApplicationDefault())
                            .build();
                } catch (Exception e) {
                    log.warn("Application default credentials not available. Creating dummy Firebase init to prevent app crash.");
                }
            }
            
            if (options != null) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Application has been initialized successfully.");
            } else {
                log.warn("Firebase Admin SDK was NOT initialized because no valid credentials could be found.");
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize Firebase Admin SDK. Push notifications will be unavailable.", e);
        }
    }
}
