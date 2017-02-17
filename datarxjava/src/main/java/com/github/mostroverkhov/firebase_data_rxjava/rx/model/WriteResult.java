package com.github.mostroverkhov.firebase_data_rxjava.rx.model;

import com.google.firebase.database.DatabaseReference;

/**
 * Created by Maksym Ostroverkhov on 07.07.2016.
 */

/**
 * Notifications for writes into database
 */
public class WriteResult {

    private final DatabaseReference databaseReference;

    public WriteResult(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
    }

    public DatabaseReference getDatabaseReference() {
        return databaseReference;
    }
}
