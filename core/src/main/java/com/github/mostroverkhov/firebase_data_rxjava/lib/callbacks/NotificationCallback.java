package com.github.mostroverkhov.firebase_data_rxjava.lib.callbacks;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

import com.github.mostroverkhov.firebase_data_rxjava.model.DataWindowChangeEvent;

/**
 * Callback for data window items change notifications
 */
public interface NotificationCallback<T> extends ErrorCallback {

    void onChildChanged(DataWindowChangeEvent<T> event);
}
