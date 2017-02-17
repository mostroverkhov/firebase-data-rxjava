package com.github.mostroverkhov.firebase_data_rxjava.lib.callbacks;

/**
 * Created by Maksym Ostroverkhov on 20.07.2016.
 */

import com.github.mostroverkhov.firebase_data_rxjava.model.NotificationResult;

/**
 * Next data window query callback
 */
public interface NextWindowCallback extends ErrorCallback {

    void onData(NotificationResult notificationResult);

}
