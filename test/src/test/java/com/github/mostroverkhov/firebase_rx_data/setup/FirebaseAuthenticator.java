package com.github.mostroverkhov.firebase_rx_data.setup;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.FirebaseOptions.Builder;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Maksym Ostroverkhov on 14.02.2017.
 */

public class FirebaseAuthenticator {

    private final String serviceAccountFileName;
    private final String databaseUrl;
    private final String uid;


    public FirebaseAuthenticator(String serviceAccountFileName,
                                 String databaseUrl,
                                 String uid) {
        this.serviceAccountFileName = serviceAccountFileName;
        this.databaseUrl = databaseUrl;
        this.uid = uid;
    }

    public void authenticate() {
        if (FirebaseApp.getApps().isEmpty()) {

            InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream(serviceAccountFileName);
            if (stream != null) {
                Map<String, Object> auth = new HashMap<>();
                auth.put("uid", uid);
                FirebaseOptions options = new Builder()
                        .setCredentials(credentials(stream))
                        .setDatabaseUrl(databaseUrl)
                        .setDatabaseAuthVariableOverride(auth)
                        .build();

                FirebaseApp.initializeApp(options);
            } else {
                throw new IllegalStateException("Error while reading service account file: "
                        + serviceAccountFileName);
            }
        }
    }

    private GoogleCredentials credentials(InputStream stream) {
        try {
            return GoogleCredentials.fromStream(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}