package com.github.mostroverkhov.firebase_rx_data.setup;

/**
 * Created by Maksym Ostroverkhov on 15.02.2017.
 */
public class Credentials {
    private final String dbUrl;
    private final String userId;
    private final String serviceFile;

    public Credentials(String dbUrl, String userId, String serviceFile) {
        this.dbUrl = dbUrl;
        this.userId = userId;
        this.serviceFile = serviceFile;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getServiceFile() {
        return serviceFile;
    }
}
