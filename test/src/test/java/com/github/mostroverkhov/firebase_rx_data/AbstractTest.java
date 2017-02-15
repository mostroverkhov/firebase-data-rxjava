package com.github.mostroverkhov.firebase_rx_data;

import com.github.mostroverkhov.firebase_rx_data.common.TestAuthenticator;

import org.junit.BeforeClass;

/**
 * Created by Maksym Ostroverkhov on 15.02.2017.
 */

public abstract class AbstractTest {

    @BeforeClass
    public static void before() throws Exception {
        TestAuthenticator testAuthenticator = new TestAuthenticator();
        testAuthenticator.authenticate();
    }
}
