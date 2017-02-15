package com.github.mostroverkhov.firebase_rx_data.lib;

import com.google.firebase.database.DatabaseError;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Firebase db access error callback
 */
public interface ErrorCallback {

    void onError(DatabaseError e);
}
