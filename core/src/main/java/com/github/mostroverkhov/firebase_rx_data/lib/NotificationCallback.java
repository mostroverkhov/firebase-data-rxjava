package com.github.mostroverkhov.firebase_rx_data.lib;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

import com.github.mostroverkhov.firebase_rx_data.model.DataWindowChangeEvent;

/**
 * Callback for notifications about data window items change
 */
public interface NotificationCallback<T> extends ErrorCallback {

    void onChildChanged(DataWindowChangeEvent<T> event);
}
