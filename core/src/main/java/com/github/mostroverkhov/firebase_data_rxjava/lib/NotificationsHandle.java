package com.github.mostroverkhov.firebase_data_rxjava.lib;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

import com.github.mostroverkhov.firebase_data_rxjava.lib.callbacks.NotificationCallback;

/**
 * Controls change notifications for data window through {@link NotificationCallback}
 */
public interface NotificationsHandle {

    void startListenToNotifications();

    void stopListenToNotifications();
}
