package com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.callbacks;

import com.google.firebase.database.DatabaseError;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * database access error callback
 */
public interface ErrorCallback {

    void onError(DatabaseError e);
}
