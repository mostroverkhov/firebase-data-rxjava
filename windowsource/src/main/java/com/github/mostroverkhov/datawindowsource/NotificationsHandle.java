package com.github.mostroverkhov.datawindowsource;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Controls change notifications for data window through
 * {@link com.github.mostroverkhov.datawindowsource.callbacks.NotificationCallback}
 */
public interface NotificationsHandle {

    void startListenToNotifications();

    void stopListenToNotifications();
}
