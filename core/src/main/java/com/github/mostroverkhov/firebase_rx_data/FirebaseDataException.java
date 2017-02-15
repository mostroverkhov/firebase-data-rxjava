package com.github.mostroverkhov.firebase_rx_data;

import com.google.firebase.database.DatabaseError;

/**
 * Created by Maksym Ostroverkhov on 11.07.2016.
 */

/**
 * Library wrapper for firebase database error
 */
public class FirebaseDataException extends RuntimeException {
    private DatabaseError databaseError;

    public FirebaseDataException() {
    }

    public FirebaseDataException(DatabaseError databaseError) {
        this.databaseError = databaseError;
    }

    public FirebaseDataException(String detailMessage, DatabaseError databaseError) {
        super(detailMessage);
        this.databaseError = databaseError;
    }

    public DatabaseError getDatabaseError() {
        return databaseError;
    }
}
