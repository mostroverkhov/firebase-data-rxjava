package com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.model;

import com.github.mostroverkhov.firebase_data_rxjava.datawindowsource.NotificationsHandle;

import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 19.07.2016.
 */

/**
 * Represents data window and notifications handle
 * */
public class DataWindowAndNotificationResult<T> extends NextQuery implements
        HasDataWindow<T> {

    public static final NotificationsHandle NOOP_CHILD_EVENTS_HANDLE = new NotificationsHandle() {
        @Override
        public void startListenToNotifications() {
            /*noop*/
        }

        @Override
        public void stopListenToNotifications() {

        }
    };
    private final List<T> data;
    private final NotificationsHandle notificationsHandle;

    public DataWindowAndNotificationResult(List<T> data,
                                    DataQuery next,
                                    NotificationsHandle notificationsHandle) {
        super(next);
        this.data = data;
        this.notificationsHandle = notificationsHandle;
    }

    public DataWindowAndNotificationResult(List<T> data,
                                           DataQuery next) {
        this(data, next, NOOP_CHILD_EVENTS_HANDLE);
    }

    @Override
    public List<T> getData() {
        return data;
    }

    public NotificationsHandle getNotificationsHandle() {
        return notificationsHandle;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Result{");
        sb.append("data=").append(data);
        sb.append(", next=").append(getNext());
        sb.append('}');
        return sb.toString();
    }

}
