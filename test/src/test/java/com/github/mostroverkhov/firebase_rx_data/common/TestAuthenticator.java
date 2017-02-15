package com.github.mostroverkhov.firebase_rx_data.common;

import com.github.mostroverkhov.firebase_rx_data.setup.Credentials;
import com.github.mostroverkhov.firebase_rx_data.setup.CredentialsFactory;
import com.github.mostroverkhov.firebase_rx_data.setup.FirebaseAuthenticator;
import com.github.mostroverkhov.firebase_rx_data.setup.PropsCredentialsFactory;

/**
 * Created by Maksym Ostroverkhov on 15.02.2017.
 */

public class TestAuthenticator {

    private final CredentialsFactory credsFactory;

    public TestAuthenticator() {
        this.credsFactory = new PropsCredentialsFactory("creds.properties");
    }

    public void authenticate() {
        Credentials creds = credsFactory.getCreds();
        FirebaseAuthenticator authenticator = new FirebaseAuthenticator(
                creds.getServiceFile(),
                creds.getDbUrl(),
                creds.getUserId());

        authenticator.authenticate();
    }
}
