package com.github.mostroverkhov.datawindowsource.callbacks;

/**
 * Created by Maksym Ostroverkhov on 20.07.2016.
 */


import com.github.mostroverkhov.datawindowsource.model.NotificationResult;

/**
 * Next data window query callback
 */
public interface NextWindowCallback extends ErrorCallback {

    void onData(NotificationResult notificationResult);

}
