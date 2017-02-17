package com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.callbacks;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Handle to control data window query in progress
 */
public interface QueryHandle {

    void cancel();
}
