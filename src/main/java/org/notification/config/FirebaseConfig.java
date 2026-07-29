package org.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

            FirebaseOptions options = buildFirebaseOptions();

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

    private FirebaseOptions buildFirebaseOptions() throws IOException {
        try (InputStream serviceAccount = resolveServiceAccountStream()) {
            if (serviceAccount != null) {
                return FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
            }
        }

        // Fallback to application default credentials
        return tryGetDefaultCredentialsOptions();
    }

    private InputStream resolveServiceAccountStream() {
        InputStream serviceAccount = tryLoadFromPath();
        if (serviceAccount == null) {
            serviceAccount = tryLoadFromClasspath();
        }
        return serviceAccount;
    }

    private InputStream tryLoadFromPath() {
        try {
            String credentialPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
            if (credentialPath != null && !credentialPath.isBlank()) {
                Path path = Paths.get(credentialPath);
                if (Files.exists(path)) {
                    log.info("Loading Firebase credentials from path: {}", credentialPath);
                    return Files.newInputStream(path);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read Firebase credentials file from path: {}", e.getMessage());
        }
        return null;
    }

    private InputStream tryLoadFromClasspath() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
        if (stream != null) {
            log.info("Loading Firebase credentials from classpath (firebase-service-account.json)");
        }
        return stream;
    }

    private FirebaseOptions tryGetDefaultCredentialsOptions() {
        try {
            log.info("No explicit Firebase credentials found. Trying Google Application Default Credentials...");
            return FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();
        } catch (Exception e) {
            log.warn("Application default credentials not available. Creating dummy Firebase init to prevent app crash.");
            return null;
        }
    }
}