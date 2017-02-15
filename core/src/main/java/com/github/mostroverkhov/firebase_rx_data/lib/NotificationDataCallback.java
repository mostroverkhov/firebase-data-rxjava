package com.github.mostroverkhov.firebase_rx_data.lib;

/**
 * Created by Maksym Ostroverkhov on 20.07.2016.
 */

import com.github.mostroverkhov.firebase_rx_data.model.NotificationResult;

/**
 * Callback for query which returns items as data window change events. Contains next data
 * window query
 */
public interface NotificationDataCallback extends ErrorCallback {

    void onData(NotificationResult notificationResult);

}
